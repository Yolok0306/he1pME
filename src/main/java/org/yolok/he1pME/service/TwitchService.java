package org.yolok.he1pME.service;

import jakarta.annotation.Nullable;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.repository.TwitchNotificationRepository;
import org.yolok.he1pME.util.CommonUtil;

import java.awt.*;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TwitchService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TwitchNotificationRepository twitchNotificationRepository;

    @Value("${twitch.api.client.id}")
    private String twitchApiClientId;

    @Value("${twitch.api.client.secret}")
    private String twitchApiClientSecret;

    @Value("${twitch.api.baseUrl}")
    private String twitchApiBaseUrl;

    @Value("${twitch.oauth.api.baseUrl}")
    private String twitchOauthApiBaseUrl;

    @Value("${twitch.channel.baseUrl}")
    private String twitchChannelBaseUrl;

    @Value("${twitch.logo.url}")
    private String twitchLogoUrl;

    private String twitchApiTokenType;

    private String twitchApiAccessToken;

    private Map<String, String> cache;

    private Map<String, Set<String>> notificationMap;

    private final Color twitchColor = new Color(144, 0, 255);

    @PostConstruct
    public void init() {
        initNotificationMap();
        getNewAccessToken();
        initCache(notificationMap.keySet());
    }

    public void initNotificationMap() {
        notificationMap = new ConcurrentHashMap<>();
        twitchNotificationRepository.findAll().parallelStream()
                .forEach(notification -> notificationMap.computeIfAbsent(
                        notification.getTwitchChannelId(), key -> ConcurrentHashMap.newKeySet()
                ).add(notification.getMessageChannelId()));
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

    @Async
    public void execute() {
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
                .map(JSONObject::valueToString)
                .map(JSONObject::new)
                .forEach(data -> {
                    String type = data.getString("type");
                    if (!StringUtils.equals(type, "live")) {
                        return;
                    }

                    String userLogin = data.getString("user_login");
                    String id = data.getString("id");
                    if (!cache.containsKey(userLogin) || !StringUtils.equals(cache.get(userLogin), id)) {
                        cache.put(userLogin, id);
                        notification(data);
                    }
                });
    }

    private void initCache(Set<String> userLoginSet) {
        String responseString = callStreamApi(userLoginSet);
        if (StringUtils.isBlank(responseString)) {
            cache = new ConcurrentHashMap<>();
            return;
        }

        JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
        cache = dataJsonArray.toList().parallelStream()
                .map(JSONObject::valueToString)
                .map(JSONObject::new)
                .filter(data -> StringUtils.equals(data.getString("type"), "live"))
                .filter(data -> CommonUtil.checkStartTime(data.getString("started_at")))
                .collect(Collectors.toConcurrentMap(data -> data.getString("user_login"), data -> data.getString("id")));
    }

    @Nullable
    private String callStreamApi(Set<String> userLoginSet) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(twitchApiBaseUrl + "streams");
        userLoginSet.parallelStream().forEach(key -> uriBuilder.queryParam("user_login", key));
        URI uri = uriBuilder.build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-Id", twitchApiClientId);
        headers.set("Authorization", twitchApiTokenType + StringUtils.SPACE + twitchApiAccessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            if (responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                getNewAccessToken();
                return callStreamApi(userLoginSet);
            }

            log.debug(responseEntity.getStatusCode() + StringUtils.SPACE + responseEntity.getBody());
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Failed to execute Twitch stream api", e);
        }

        return null;
    }

    private void getNewAccessToken() {
        URI uri = UriComponentsBuilder.fromUriString(twitchOauthApiBaseUrl + "token")
                .queryParam("client_id", twitchApiClientId)
                .queryParam("client_secret", twitchApiClientSecret)
                .queryParam("grant_type", "client_credentials")
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
            JSONObject response = new JSONObject(Objects.requireNonNull(responseEntity.getBody()));
            twitchApiAccessToken = response.getString("access_token");
            twitchApiTokenType = StringUtils.capitalize(response.getString("token_type"));
        } catch (Exception e) {
            log.error("Failed to execute Twitch get new access token api", e);
        }
    }


    private void notification(JSONObject data) {
        String userLogin = data.getString("user_login");
        String userName = data.getString("user_name");
        String title = data.getString("title");
        String image = data.getString("thumbnail_url").replace("-{width}x{height}", StringUtils.EMPTY);
        notificationMap.get(userLogin).parallelStream().forEach(messageChannelId -> {
            MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                return;
            }

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setTitle(title, twitchChannelBaseUrl + userLogin)
                    .setImage(image)
                    .setColor(twitchColor)
                    .setAuthor(userName, null, twitchLogoUrl)
                    .build();
            messageChannel.sendMessageEmbeds(messageEmbed).queue();
        });
    }
}
