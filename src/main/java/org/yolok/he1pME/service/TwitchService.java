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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.util.CommonUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TwitchService {

    private final WebClient webClient;

    private final Map<String, Set<String>> twitchNotificationMap;

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

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    private final Color twitchColor = new Color(144, 0, 255);

    public TwitchService(WebClient webClient,
                         @Qualifier("twitchNotificationMap") Map<String, Set<String>> twitchNotificationMap) {
        this.webClient = webClient;
        this.twitchNotificationMap = twitchNotificationMap;
    }

    @PostConstruct
    public void init() {
        getNewAccessToken()
                .filter(Boolean::booleanValue)
                .flatMap(success -> {
                    return initCache(twitchNotificationMap.keySet());
                })
                .subscribe();
    }

    public void adjustCache() {
        if (twitchNotificationMap.isEmpty()) {
            cache.clear();
            return;
        }

        cache.keySet().retainAll(twitchNotificationMap.keySet());
        Set<String> missing = twitchNotificationMap.keySet().stream()
                .filter(id -> {
                    return !cache.containsKey(id);
                })
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            initCache(missing).subscribe();
        }
    }

    @Async
    public void execute() {
        if (twitchNotificationMap.isEmpty() || !isExecuting.compareAndSet(false, true)) {
            return;
        }

        callStreamApi(twitchNotificationMap.keySet())
                .doOnNext(this::processStreamResponse)
                .doOnError(e -> {
                    log.error("TwitchService execution failed: ", e);
                })
                .doFinally(signal -> {
                    isExecuting.set(false);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void processStreamResponse(String responseString) {
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        try {
            JSONObject json = new JSONObject(responseString);
            JSONArray dataArray = json.optJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                return;
            }

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject streamData = dataArray.getJSONObject(i);
                if (!"live".equals(streamData.optString("type"))) {
                    continue;
                }

                String userLogin = streamData.getString("user_login");
                String streamId = streamData.getString("id");

                if (!cache.containsKey(userLogin) || !StringUtils.equals(cache.get(userLogin), streamId)) {
                    cache.put(userLogin, streamId);
                    notifyChannels(streamData);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process stream response: ", e);
        }
    }

    private Mono<Void> initCache(Set<String> userLoginSet) {
        return callStreamApi(userLoginSet)
                .doOnNext(response -> {
                    if (StringUtils.isBlank(response)) {
                        return;
                    }
                    try {
                        JSONArray dataArray = new JSONObject(response).optJSONArray("data");
                        if (dataArray == null) {
                            return;
                        }
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject data = dataArray.getJSONObject(i);
                            if ("live".equals(data.optString("type")) && CommonUtil.checkStartTime(data.getString("started_at"))) {
                                cache.put(data.getString("user_login"), data.getString("id"));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to init cache: ", e);
                    }
                })
                .then();
    }

    private Mono<String> callStreamApi(Set<String> userLoginSet) {
        return callStreamApiWithRetry(userLoginSet, true);
    }

    private Mono<String> callStreamApiWithRetry(Set<String> userLoginSet, boolean allowRetry) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(twitchApiBaseUrl).path("streams");
        userLoginSet.forEach(login -> {
            builder.queryParam("user_login", login);
        });
        URI uri = builder.build().toUri();

        return webClient.get()
                .uri(uri)
                .header("Client-Id", twitchApiClientId)
                .header("Authorization", twitchApiTokenType + " " + twitchApiAccessToken)
                .retrieve()
                .onStatus(status -> {
                    return status.value() == 401;
                }, response -> {
                    return Mono.error(new RuntimeException("Unauthorized"));
                })
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    if ("Unauthorized".equals(e.getMessage()) && allowRetry) {
                        return getNewAccessToken()
                                .filter(Boolean::booleanValue)
                                .flatMap(success -> {
                                    return callStreamApiWithRetry(userLoginSet, false);
                                });
                    }
                    return Mono.error(e);
                })
                .doOnError(e -> {
                    if (!"Unauthorized".equals(e.getMessage())) {
                        log.error("Twitch API request failed: ", e);
                    }
                });
    }

    private Mono<Boolean> getNewAccessToken() {
        URI uri = UriComponentsBuilder.fromHttpUrl(twitchOauthApiBaseUrl).path("token")
                .queryParam("client_id", twitchApiClientId)
                .queryParam("client_secret", twitchApiClientSecret)
                .queryParam("grant_type", "client_credentials")
                .build().toUri();

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    JSONObject json = new JSONObject(Objects.requireNonNull(response));
                    twitchApiAccessToken = json.getString("access_token");
                    twitchApiTokenType = StringUtils.capitalize(json.getString("token_type"));
                    return true;
                })
                .onErrorResume(e -> {
                    log.error("Failed to refresh Twitch access token: ", e);
                    return Mono.just(false);
                });
    }

    private void notifyChannels(JSONObject data) {
        String userLogin = data.getString("user_login");
        String userName = data.getString("user_name");
        String title = data.getString("title");
        String image = data.getString("thumbnail_url").replace("-{width}x{height}", "1280x720");
        
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(title, twitchChannelBaseUrl + userLogin)
                .setImage(image)
                .setColor(twitchColor)
                .setAuthor(userName, null, twitchLogoUrl)
                .build();

        Set<String> channelIds = twitchNotificationMap.get(userLogin);
        if (channelIds != null) {
            channelIds.forEach(channelId -> {
                MessageChannel channel = CommonUtil.JDA.getChannelById(MessageChannel.class, channelId);
                if (channel != null) {
                    channel.sendMessageEmbeds(embed).queue(null, e -> {
                        log.error("Failed to send Twitch notify to channel {}: ", channelId, e);
                    });
                }
            });
        }
    }
}
