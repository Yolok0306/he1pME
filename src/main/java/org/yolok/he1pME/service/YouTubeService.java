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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.yolok.he1pME.util.CommonUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Service
public class YouTubeService {

    private final WebClient webClient;

    private final Map<String, Set<String>> youtubeNotificationMap;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    @Value("${youtube.api.baseUrl}")
    private String youtubeApiBaseUrl;

    @Value("${youtube.video.baseUrl}")
    private String youtubeVideoBaseUrl;

    @Value("${youtube.logo.url}")
    private String youtubeLogoUrl;

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    private final Color youtubeColor = new Color(255, 0, 0);

    public YouTubeService(WebClient webClient,
                          @Qualifier("youtubeNotificationMap") Map<String, Set<String>> youtubeNotificationMap) {
        this.webClient = webClient;
        this.youtubeNotificationMap = youtubeNotificationMap;
    }

    @PostConstruct
    public void init() {
        fetchPlaylistItems(null)
                .collectList()
                .subscribe(this::initCache);
    }

    public void adjustCache() {
        if (youtubeNotificationMap.isEmpty()) {
            cache.clear();
            return;
        }

        cache.keySet().retainAll(youtubeNotificationMap.keySet());
        fetchPlaylistItems(cache.keySet())
                .collectList()
                .subscribe(this::initCache);
    }

    @Async
    public void execute() {
        if (youtubeNotificationMap.isEmpty() || !isExecuting.compareAndSet(false, true)) {
            return;
        }

        fetchPlaylistItems(null)
                .collectList()
                .flatMap(responses -> {
                    Map<String, Set<String>> pending = extractNewVideos(responses);
                    if (pending.isEmpty()) {
                        return Mono.empty();
                    }

                    return callVideoApi(pending.keySet())
                            .doOnNext(videoData -> processNotifications(videoData, pending));
                })
                .doOnError(e -> {
                    log.error("YouTubeService execution failed: ", e);
                })
                .doFinally(signal -> {
                    isExecuting.set(false);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Flux<String> fetchPlaylistItems(@Nullable Set<String> ids) {
        Set<String> targets = (ids == null || ids.isEmpty()) ? youtubeNotificationMap.keySet() : ids;
        return Flux.fromIterable(targets)
                .flatMap(this::callPlayListItemApi);
    }

    private Map<String, Set<String>> extractNewVideos(java.util.List<String> responses) {
        Map<String, Set<String>> newVideos = new java.util.HashMap<>();
        for (String response : responses) {
            JSONObject snippet = getFirstItemSnippet(response);
            if (snippet == null) {
                continue;
            }

            String playlistId = snippet.getString("playlistId");
            String videoId = snippet.getJSONObject("resourceId").getString("videoId");
            if (!StringUtils.equals(cache.get(playlistId), videoId)) {
                if (cache.containsKey(playlistId) || CommonUtil.checkStartTime(snippet.getString("publishedAt"))) {
                    newVideos.put(videoId, youtubeNotificationMap.get(playlistId));
                }
                cache.put(playlistId, videoId);
            }
        }
        return newVideos;
    }

    @Nullable
    private JSONObject getFirstItemSnippet(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray items = json.optJSONArray("items");
            return (items != null && !items.isEmpty()) ? items.getJSONObject(0).optJSONObject("snippet") : null;
        } catch (Exception e) {
            log.error("Failed to parse first item snippet: ", e);
            return null;
        }
    }

    private void processNotifications(String videoData, Map<String, Set<String>> pendingNotifications) {
        try {
            JSONArray items = new JSONObject(videoData).optJSONArray("items");
            if (items == null) {
                return;
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (isLiveStreamEnded(item)) {
                    continue;
                }

                String videoId = item.getString("id");
                sendEmbedToChannels(item.getJSONObject("snippet"), videoId, pendingNotifications.get(videoId));
            }
        } catch (Exception e) {
            log.error("Failed to process notifications: ", e);
        }
    }

    private void sendEmbedToChannels(JSONObject snippet, String videoId, Set<String> channelIds) {
        if (channelIds == null) {
            return;
        }

        try {
            MessageEmbed embed = createEmbed(snippet, videoId);
            channelIds.forEach(id -> {
                MessageChannel channel = CommonUtil.JDA.getChannelById(MessageChannel.class, id);
                if (channel != null) {
                    channel.sendMessageEmbeds(embed).queue(null, e -> {
                        log.error("Send failed to channel {}: ", id, e);
                    });
                }
            });
        } catch (Exception e) {
            log.error("Failed to send embed to channels: ", e);
        }
    }

    private MessageEmbed createEmbed(JSONObject snippet, String videoId) {
        return new EmbedBuilder()
                .setTitle(snippet.getString("title"), youtubeVideoBaseUrl + videoId)
                .setImage(getThumbnail(snippet.getJSONObject("thumbnails")))
                .setColor(youtubeColor)
                .setAuthor(snippet.getString("channelTitle"), null, youtubeLogoUrl)
                .build();
    }

    private void initCache(java.util.List<String> responses) {
        for (String res : responses) {
            JSONObject snippet = getFirstItemSnippet(res);
            if (snippet != null && CommonUtil.checkStartTime(snippet.getString("publishedAt"))) {
                cache.put(snippet.getString("playlistId"), snippet.getJSONObject("resourceId").getString("videoId"));
            }
        }
    }

    private Mono<String> callPlayListItemApi(String playlistId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(youtubeApiBaseUrl)
                .path("playlistItems")
                .queryParam("playlistId", playlistId)
                .queryParam("part", "snippet")
                .queryParam("maxResults", "1")
                .queryParam("key", youtubeApiKey)
                .build().toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.warn("YouTube API error (playlistId: {}): ", playlistId, e);
                    return Mono.empty();
                });
    }

    private Mono<String> callVideoApi(Set<String> ids) {
        URI uri = UriComponentsBuilder.fromHttpUrl(youtubeApiBaseUrl)
                .path("videos")
                .queryParam("part", "snippet,liveStreamingDetails")
                .queryParam("key", youtubeApiKey)
                .queryParam("id", String.join(",", ids))
                .build().toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("YouTube Video API error: ", e);
                    return Mono.empty();
                });
    }

    private boolean isLiveStreamEnded(JSONObject item) {
        return item.has("liveStreamingDetails") && item.getJSONObject("liveStreamingDetails").has("actualEndTime");
    }

    private String getThumbnail(JSONObject thumbnails) {
        return Stream.of("maxres", "standard", "high", "medium")
                .filter(thumbnails::has)
                .findFirst()
                .map(k -> thumbnails.getJSONObject(k).getString("url"))
                .orElse(thumbnails.getJSONObject("default").getString("url"));
    }
}
