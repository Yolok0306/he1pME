package service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.CommonUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class YouTubeService implements Runnable {
    public static Map<String, String> YOUTUBE_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, Set<String>> YOUTUBE_NOTIFICATION_MAP = new ConcurrentHashMap<>();

    @Override
    public void run() {
        if (YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final Set<String> playlistItemResponseSet = YOUTUBE_NOTIFICATION_MAP.keySet().parallelStream()
                .map(YouTubeService::callPlayListItemApi).collect(Collectors.toSet());
        try {
            final Map<String, Set<String>> needToBeNotifiedMap = constructNeedToBeNotifiedMap(playlistItemResponseSet);
            if (needToBeNotifiedMap.isEmpty()) {
                return;
            }

            final String videoResponseString = callVideoApi(needToBeNotifiedMap.keySet());
            if (StringUtils.isBlank(videoResponseString)) {
                return;
            }

            final JSONArray itemJsonArray = new JSONObject(videoResponseString).getJSONArray("items");
            itemJsonArray.toList().parallelStream()
                    .filter(item -> item instanceof HashMap<?, ?>)
                    .map(item -> (Map<?, ?>) item)
                    .forEach(item -> notification(item, needToBeNotifiedMap));
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    public static String callPlayListItemApi(final String playlistId) {
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final URI uri = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/playlistItems")
                    .addParameter("playlistId", playlistId)
                    .addParameter("part", "snippet")
                    .addParameter("maxResults", "1")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY)
                    .build();
            final HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(uri).build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body().replaceAll(StringUtils.LF, StringUtils.EMPTY));
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private Map<String, Set<String>> constructNeedToBeNotifiedMap(final Set<String> playlistItemResponseSet) {
        final Map<String, Set<String>> newVideoIdMap = new HashMap<>();
        playlistItemResponseSet.parallelStream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .forEach(snippetJsonObject -> {
                    final String playlistId = snippetJsonObject.getString("playlistId");
                    final String videoId = snippetJsonObject.getJSONObject("resourceId").getString("videoId");
                    if (StringUtils.equals(YOUTUBE_CACHE.get(playlistId), videoId)) {
                        return;
                    }

                    YOUTUBE_CACHE.put(playlistId, videoId);
                    newVideoIdMap.putIfAbsent(videoId, YOUTUBE_NOTIFICATION_MAP.get(playlistId));
                });
        return newVideoIdMap;
    }

    private String callVideoApi(final Set<String> videoIdSet) {
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/videos")
                    .addParameter("part", "snippet,liveStreamingDetails")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY);
            videoIdSet.parallelStream().forEach(videoId -> uriBuilder.addParameter("id", videoId));
            final HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(uriBuilder.build()).build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body().replaceAll(StringUtils.LF, StringUtils.EMPTY));
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private void notification(final Map<?, ?> item, final Map<String, Set<String>> needToBeNotifiedMap) {
        if (item.containsKey("liveStreamingDetails") && ((Map<?, ?>) item.get("liveStreamingDetails")).containsKey("actualEndTime")) {
            return;
        }

        final Map<?, ?> snippet = (Map<?, ?>) item.get("snippet");
        final String videoId = item.get("id").toString();
        final String title = snippet.get("channelTitle").toString();
        final String desc = snippet.get("title").toString();
        final String thumb = getThumbnail((Map<?, ?>) snippet.get("thumbnails"));
        final Color color = new Color(255, 0, 0);
        needToBeNotifiedMap.get(videoId).parallelStream().forEach(messageChannelId -> {
            final MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                return;
            }

            final MessageEmbed messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb)
                    .setColor(color).setAuthor("Youtube", null, CommonUtil.YOUTUBE_LOGO_URI).build();
            messageChannel.sendMessage("https://www.youtube.com/watch?v=" + videoId).addEmbeds(messageEmbed).queue();
        });
    }

    private String getThumbnail(final Map<?, ?> thumbnail) {
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

    public static void addDataToYoutubeCache(final Set<String> playlistItemResponseSet) {
        playlistItemResponseSet.parallelStream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .filter(snippetJsonObject -> CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt")))
                .forEach(snippetJsonObject -> {
                    final String playlistId = snippetJsonObject.getString("playlistId");
                    final String videoId = snippetJsonObject.getJSONObject("resourceId").getString("videoId");
                    YOUTUBE_CACHE.putIfAbsent(playlistId, videoId);
                });
    }
}
