package org.yolok.he1pME.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.entity.TwitchNotification;
import org.yolok.he1pME.repository.TwitchNotificationRepository;
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
public class TwitchService implements Runnable {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${twitch.api.client.id}")
    private String twitchApiClientId;
    @Value("${twitch.api.client.secret}")
    private String twitchApiClientSecret;
    @Value("${twitch.api.base.uri}")
    private String twitchApiBaseUri;
    @Value("${twitch.logo.uri:null}")
    private String twitchLogoUri;
    private String twitchApiTokenType;
    private String twitchApiAccessToken;
    private Map<String, String> cache;
    private Map<String, Set<String>> notificationMap;
    @Autowired
    private TwitchNotificationRepository twitchNotificationRepository;

    @PostConstruct
    public void init() {
        initNotificationMap();
        getNewAccessToken();
        initCache(notificationMap.keySet());
    }

    public void initNotificationMap() {
        notificationMap = new HashMap<>();
        Iterable<TwitchNotification> twitchNotificationIterable = twitchNotificationRepository.findAll();
        if (!twitchNotificationIterable.iterator().hasNext()) {
            return;
        }

        twitchNotificationIterable.forEach(twitchNotification -> {
            String key = twitchNotification.getTwitchChannelId();
            notificationMap.computeIfAbsent(key, (k) -> new HashSet<>());
            Set<String> value = notificationMap.get(key);
            value.add(twitchNotification.getMessageChannelId());
        });
    }

    public void adjustCache() {
        if (notificationMap.isEmpty()) {
            cache.clear();
            return;
        }

        Map<String, String> existingDataMap = cache.entrySet().parallelStream()
                .filter(entry -> notificationMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<String> newDataSet = notificationMap.keySet().parallelStream()
                .filter(key -> !existingDataMap.containsKey(key))
                .collect(Collectors.toSet());
        initCache(newDataSet);
        cache.putAll(existingDataMap);
    }

    private void initCache(Set<String> userLoginSet) {
        String responseString = callStreamApi(userLoginSet);
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
        if (dataJsonArray.isEmpty()) {
            return;
        }

        cache = dataJsonArray.toList().parallelStream()
                .filter(data -> data instanceof HashMap<?, ?>)
                .map(data -> (Map<?, ?>) data)
                .filter(data -> StringUtils.equals(data.get("type").toString(), "live"))
                .filter(data -> CommonUtil.checkStartTime(data.get("started_at").toString()))
                .collect(Collectors.toMap(data -> data.get("user_login").toString(), data -> data.get("id").toString(),
                        (existingValue, newValue) -> existingValue, ConcurrentHashMap::new));
    }

    @Override
    public void run() {
        if (notificationMap.isEmpty()) {
            return;
        }

        String responseString = callStreamApi(notificationMap.keySet());
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
        if (dataJsonArray.isEmpty()) {
            return;
        }

        dataJsonArray.toList().parallelStream()
                .filter(data -> data instanceof HashMap<?, ?>)
                .map(data -> (Map<?, ?>) data)
                .forEach(data -> {
                    String type = data.get("type").toString();
                    if (!StringUtils.equals(type, "live")) {
                        return;
                    }

                    String userLogin = data.get("user_login").toString();
                    String id = data.get("id").toString();
                    if (!cache.containsKey(userLogin) || !StringUtils.equals(cache.get(userLogin), id)) {
                        cache.put(userLogin, id);
                        notification(data);
                    }
                });
    }

    private String callStreamApi(Set<String> userLoginSet) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(twitchApiBaseUri + "/streams");
        userLoginSet.parallelStream().forEach(key -> uriBuilder.queryParam("user_login", key));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-Id", twitchApiClientId);
        headers.set("Authorization", twitchApiTokenType + StringUtils.SPACE + twitchApiAccessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                uriBuilder.build().toUri(), HttpMethod.GET, entity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            getNewAccessToken();
            return callStreamApi(userLoginSet);
        }

        log.info(responseEntity.getStatusCode() + StringUtils.SPACE + responseEntity.getBody());
        return responseEntity.getBody();
    }

    private void getNewAccessToken() {
        String tokenEndpoint = "https://id.twitch.tv/oauth2/token";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(tokenEndpoint)
                .queryParam("client_id", twitchApiClientId)
                .queryParam("client_secret", twitchApiClientSecret)
                .queryParam("grant_type", "client_credentials");
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                uriBuilder.build().toUri(), HttpMethod.POST, entity, String.class);
        JSONObject response = new JSONObject(responseEntity.getBody());
        twitchApiAccessToken = response.getString("access_token");
        twitchApiTokenType = StringUtils.capitalize(response.getString("token_type"));
    }


    private void notification(Map<?, ?> data) {
        String userLogin = data.get("user_login").toString();
        String title = data.get("user_name").toString();
        String desc = data.get("title").toString();
        String thumb = data.get("thumbnail_url").toString().replace("-{width}x{height}", StringUtils.EMPTY);
        Color color = new Color(144, 0, 255);

        for (String messageChannelId : notificationMap.get(userLogin)) {
            MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                continue;
            }

            MessageEmbed messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb)
                    .setColor(color).setAuthor("Twitch", null, twitchLogoUri).build();
            messageChannel.sendMessage("https://www.twitch.tv/" + userLogin).addEmbeds(messageEmbed).queue();
        }
    }
}
