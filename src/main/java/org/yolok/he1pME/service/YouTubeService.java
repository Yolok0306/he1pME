package org.yolok.he1pME.service;

import jakarta.annotation.Nullable;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.entity.YouTubeNotification;
import org.yolok.he1pME.repository.YouTubeNotificationRepository;
import org.yolok.he1pME.util.CommonUtil;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class YouTubeService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private YouTubeNotificationRepository youTubeNotificationRepository;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    @Value("${youtube.api.baseUrl}")
    private String youtubeApiBaseUrl;

    @Value("${youtube.video.baseUrl}")
    private String youtubeVideoBaseUrl;

    @Value("${youtube.logo.url}")
    private String youtubeLogoUrl;

    private Map<String, String> cache;

    private Map<String, Set<String>> notificationMap;

    private final Color youtubeColor = new Color(255, 0, 0);

    @PostConstruct
    public void init() {
        initNotificationMap();
        Set<String> playlistItemResponseSet = filterAndGetPlayListItemResponseSet(null);
        initCache(playlistItemResponseSet);
    }

    public void initNotificationMap() {
        List<YouTubeNotification> youTubeNotificationList = youTubeNotificationRepository.findAll();
        notificationMap = CollectionUtils.isEmpty(youTubeNotificationList) ?
                Collections.emptyMap() :
                youTubeNotificationList.parallelStream().collect(Collectors.groupingBy(
                        YouTubeNotification::getYoutubeChannelPlaylistId,
                        Collectors.mapping(YouTubeNotification::getMessageChannelId, Collectors.toSet())
                ));
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

    @Async
    public void execute() {
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
                .map(JSONObject::valueToString)
                .map(JSONObject::new)
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
                        .map(this::callPlayListItemApi)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()) :
                notificationMap.keySet().parallelStream()
                        .filter(key -> !existingDataSet.contains(key))
                        .map(this::callPlayListItemApi)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    @Nullable
    private String callPlayListItemApi(String playlistId) {
        URI uri = UriComponentsBuilder.fromUriString(youtubeApiBaseUrl + "playlistItems")
                .queryParam("playlistId", playlistId)
                .queryParam("part", "snippet")
                .queryParam("maxResults", "1")
                .queryParam("key", youtubeApiKey)
                .build()
                .toUri();
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            log.debug(responseEntity.getStatusCode() + StringUtils.SPACE + handleResponseBodyLog(responseEntity.getBody()));
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Failed to execute Youtube play list item api", e);
        }

        return null;
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

    @Nullable
    private String callVideoApi(Set<String> videoIdSet) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(youtubeApiBaseUrl + "videos")
                .queryParam("part", "snippet,liveStreamingDetails")
                .queryParam("key", youtubeApiKey);
        videoIdSet.parallelStream().forEach(videoId -> uriBuilder.queryParam("id", videoId));
        URI uri = uriBuilder.build().toUri();
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            log.debug(responseEntity.getStatusCode() + StringUtils.SPACE + handleResponseBodyLog(responseEntity.getBody()));
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Failed to execute Youtube video api", e);
        }

        return null;
    }

    private String handleResponseBodyLog(String responseBody) {
        return responseBody == null ? StringUtils.EMPTY : responseBody.replaceAll(StringUtils.LF, StringUtils.EMPTY);
    }

    private void notification(JSONObject item, Map<String, Set<String>> needToBeNotifiedMap) {
        if (item.has("liveStreamingDetails") && item.getJSONObject("liveStreamingDetails").has("actualEndTime")) {
            return;
        }

        JSONObject snippet = item.getJSONObject("snippet");
        String videoId = item.getString("id");
        String channelTitle = snippet.getString("channelTitle");
        String videoTitle = snippet.getString("title");
        String image = getImage(snippet.getJSONObject("thumbnails"));
        needToBeNotifiedMap.get(videoId).parallelStream().forEach(messageChannelId -> {
            MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                return;
            }

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setTitle(videoTitle, youtubeVideoBaseUrl + videoId)
                    .setImage(image)
                    .setColor(youtubeColor)
                    .setAuthor(channelTitle, null, youtubeLogoUrl)
                    .build();
            messageChannel.sendMessageEmbeds(messageEmbed).queue();
        });
    }

    private String getImage(JSONObject thumbnail) {
        String maxSizeThumbnail = Stream.of("maxres", "standard", "high", "medium")
                .filter(thumbnail::has)
                .findFirst()
                .orElse("default");
        return thumbnail.getJSONObject(maxSizeThumbnail).getString("url");
    }
}
