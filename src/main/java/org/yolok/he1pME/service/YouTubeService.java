package org.yolok.he1pME.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.entity.YouTubeNotification;
import org.yolok.he1pME.repository.YouTubeNotificationRepository;
import org.yolok.he1pME.util.CommonUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTubeService implements Runnable {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${youtube.api.key}")
    private String youtubeApiKey;
    @Value("${youtube.api.base.uri}")
    private String youtubeApiBaseUri;
    @Value("${youtube.logo.uri:null}")
    private String youtubeLogoUri;
    private Map<String, String> cache;
    private Map<String, Set<String>> notificationMap;
    @Autowired
    private YouTubeNotificationRepository youTubeNotificationRepository;

    @PostConstruct
    public void init() {
        initNotificationMap();
        Set<String> playlistItemResponseSet = filterAndGetPlayListItemResponseSet(null);
        initCache(playlistItemResponseSet);
    }

    public void initNotificationMap() {
        notificationMap = new HashMap<>();
        Iterable<YouTubeNotification> youTubeNotificationIterable = youTubeNotificationRepository.findAll();
        if (!youTubeNotificationIterable.iterator().hasNext()) {
            return;
        }

        youTubeNotificationIterable.forEach(youTubeNotification -> {
            String key = youTubeNotification.getYoutubeChannelPlaylistId();
            notificationMap.computeIfAbsent(key, (k) -> new HashSet<>());
            Set<String> value = notificationMap.get(key);
            value.add(youTubeNotification.getMessageChannelId());
        });
    }

    public void adjustCache() {
        if (notificationMap.isEmpty()) {
            cache.clear();
            return;
        }

        Map<String, String> existingDataMap = cache.entrySet().parallelStream()
                .filter(entry -> notificationMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existingValue, newValue) -> existingValue, ConcurrentHashMap::new));
        Set<String> newDataSet = filterAndGetPlayListItemResponseSet(existingDataMap.keySet());
        initCache(newDataSet);
        cache.putAll(existingDataMap);
    }

    @Override
    public void run() {
        if (notificationMap.isEmpty()) {
            return;
        }

        Set<String> playlistItemResponseSet = filterAndGetPlayListItemResponseSet(null);
        Map<String, Set<String>> needToBeNotifiedMap = constructNeedToBeNotifiedMap(playlistItemResponseSet);
        if (needToBeNotifiedMap.isEmpty()) {
            return;
        }

        String videoResponseString = callVideoApi(needToBeNotifiedMap.keySet());
        if (StringUtils.isBlank(videoResponseString)) {
            return;
        }

        JSONArray itemJsonArray = new JSONObject(videoResponseString).getJSONArray("items");
        itemJsonArray.toList().parallelStream()
                .filter(item -> item instanceof HashMap<?, ?>)
                .map(item -> (Map<?, ?>) item)
                .forEach(item -> notification(item, needToBeNotifiedMap));
    }

    private void initCache(Set<String> playlistItemResponseSet) {
        cache = playlistItemResponseSet.parallelStream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .filter(snippetJsonObject -> CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt")))
                .collect(Collectors.toMap(
                        snippetJsonObject -> snippetJsonObject.getString("playlistId"),
                        snippetJsonObject -> snippetJsonObject.getJSONObject("resourceId").getString("videoId"),
                        (existingValue, newValue) -> existingValue,
                        ConcurrentHashMap::new
                ));
    }

    private Set<String> filterAndGetPlayListItemResponseSet(Set<String> existingDataSet) {
        return CollectionUtils.isEmpty(existingDataSet) ?
                notificationMap.keySet().parallelStream()
                        .map(this::callPlayListItemApi).collect(Collectors.toSet()) :
                notificationMap.keySet().parallelStream().filter(key -> !existingDataSet.contains(key))
                        .map(this::callPlayListItemApi).collect(Collectors.toSet());
    }

    private String callPlayListItemApi(String playlistId) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(youtubeApiBaseUri + "/playlistItems")
                .queryParam("playlistId", playlistId)
                .queryParam("part", "snippet")
                .queryParam("maxResults", "1")
                .queryParam("key", youtubeApiKey);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriBuilder.build().toUri(), String.class);
        log.info(responseEntity.getStatusCode() + StringUtils.SPACE + handleResponseBodyLog(responseEntity.getBody()));
        return responseEntity.getBody();
    }


    private Map<String, Set<String>> constructNeedToBeNotifiedMap(Set<String> playlistItemResponseSet) {
        Map<String, Set<String>> newVideoIdMap = new HashMap<>();
        playlistItemResponseSet.parallelStream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .forEach(snippetJsonObject -> {
                    String playlistId = snippetJsonObject.getString("playlistId");
                    String videoId = snippetJsonObject.getJSONObject("resourceId").getString("videoId");
                    if (StringUtils.equals(cache.get(playlistId), videoId)) {
                        return;
                    }

                    cache.put(playlistId, videoId);
                    newVideoIdMap.putIfAbsent(videoId, notificationMap.get(playlistId));
                });
        return newVideoIdMap;
    }

    private String callVideoApi(Set<String> videoIdSet) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(youtubeApiBaseUri + "/videos")
                .queryParam("part", "snippet,liveStreamingDetails")
                .queryParam("key", youtubeApiKey);
        videoIdSet.parallelStream().forEach(videoId -> uriBuilder.queryParam("id", videoId));
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriBuilder.build().toUri(), String.class);
        log.info(responseEntity.getStatusCode() + StringUtils.SPACE + handleResponseBodyLog(responseEntity.getBody()));
        return responseEntity.getBody();
    }

    private String handleResponseBodyLog(String responseBody) {
        return responseBody == null ? StringUtils.EMPTY : responseBody.replaceAll(StringUtils.LF, StringUtils.EMPTY);
    }

    private void notification(Map<?, ?> item, Map<String, Set<String>> needToBeNotifiedMap) {
        if (item.containsKey("liveStreamingDetails") && ((Map<?, ?>) item.get("liveStreamingDetails")).containsKey("actualEndTime")) {
            return;
        }

        Map<?, ?> snippet = (Map<?, ?>) item.get("snippet");
        String videoId = item.get("id").toString();
        String title = snippet.get("channelTitle").toString();
        String desc = snippet.get("title").toString();
        String thumb = getThumbnail((Map<?, ?>) snippet.get("thumbnails"));
        Color color = new Color(255, 0, 0);
        needToBeNotifiedMap.get(videoId).parallelStream().forEach(messageChannelId -> {
            MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                return;
            }

            MessageEmbed messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb)
                    .setColor(color).setAuthor("Youtube", null, youtubeLogoUri).build();
            messageChannel.sendMessage("https://www.youtube.com/watch?v=" + videoId).addEmbeds(messageEmbed).queue();
        });
    }

    private String getThumbnail(Map<?, ?> thumbnail) {
        if (thumbnail.containsKey("maxres")) {
            return ((Map<?, ?>) thumbnail.get("maxres")).get("url").toString();
        } else if (thumbnail.containsKey("standard")) {
            return ((Map<?, ?>) thumbnail.get("standard")).get("url").toString();
        } else if (thumbnail.containsKey("high")) {
            return ((Map<?, ?>) thumbnail.get("high")).get("url").toString();
        } else if (thumbnail.containsKey("medium")) {
            return ((Map<?, ?>) thumbnail.get("medium")).get("url").toString();
        } else {
            return ((Map<?, ?>) thumbnail.get("default")).get("url").toString();
        }
    }
}
