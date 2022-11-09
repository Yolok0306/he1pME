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
import java.util.stream.Collectors;

@Slf4j
public class YouTubeService {
    public static Map<String, String> YT_PLAYLIST_ID_VIDEO_ID_MAP = new HashMap<>();
    public static final Map<String, Set<String>> YOUTUBE_NOTIFICATION_MAP = new HashMap<>();

    protected void execute() {
        if (YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final Set<String> playlistItemResponseSet = YOUTUBE_NOTIFICATION_MAP.keySet().stream()
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
            for (int i = 0; i < itemJsonArray.length(); i++) {
                notification(itemJsonArray.getJSONObject(i), needToBeNotifiedMap);
            }
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
        playlistItemResponseSet.stream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .forEach(snippetJsonObject -> {
                    final String playlistId = snippetJsonObject.getString("playlistId");
                    final String videoId = snippetJsonObject.getJSONObject("resourceId").getString("videoId");
                    if (StringUtils.equals(YT_PLAYLIST_ID_VIDEO_ID_MAP.get(playlistId), videoId)) {
                        return;
                    }

                    YT_PLAYLIST_ID_VIDEO_ID_MAP.put(playlistId, videoId);
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
            videoIdSet.forEach(videoId -> uriBuilder.addParameter("id", videoId));
            final HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(uriBuilder.build()).build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body().replaceAll(StringUtils.LF, StringUtils.EMPTY));
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private void notification(final JSONObject videoJsonObject, final Map<String, Set<String>> needToBeNotifiedMap) {
        if (videoJsonObject.has("liveStreamingDetails") && videoJsonObject.getJSONObject("liveStreamingDetails").has("actualEndTime")) {
            return;
        }

        final JSONObject snippetJsonObject = videoJsonObject.getJSONObject("snippet");
        final String videoId = videoJsonObject.getString("id");
        final String title = snippetJsonObject.getString("channelTitle");
        final String desc = snippetJsonObject.getString("title");
        final String thumb = getThumbnail(snippetJsonObject.getJSONObject("thumbnails"));
        final Color color = new Color(255, 0, 0);

        for (final String messageChannelId : needToBeNotifiedMap.get(videoId)) {
            final MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                continue;
            }

            final MessageEmbed messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb)
                    .setColor(color).setAuthor("Youtube", null, CommonUtil.YOUTUBE_LOGO_URI).build();
            messageChannel.sendMessage("https://www.youtube.com/watch?v=" + videoId).addEmbeds(messageEmbed).queue();
        }
    }

    private String getThumbnail(final JSONObject thumbnailJsonObject) {
        if (thumbnailJsonObject.has("maxres")) {
            return thumbnailJsonObject.getJSONObject("maxres").getString("url");
        } else if (thumbnailJsonObject.has("standard")) {
            return thumbnailJsonObject.getJSONObject("standard").getString("url");
        } else if (thumbnailJsonObject.has("high")) {
            return thumbnailJsonObject.getJSONObject("high").getString("url");
        } else if (thumbnailJsonObject.has("medium")) {
            return thumbnailJsonObject.getJSONObject("medium").getString("url");
        } else {
            return thumbnailJsonObject.getJSONObject("default").getString("url");
        }
    }

    public static void addDataToYTPlaylistIdVideoIdMap(final Set<String> playlistItemResponseSet) {
        playlistItemResponseSet.stream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .filter(snippetJsonObject -> CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt")))
                .forEach(snippetJsonObject -> {
                    final String playlistId = snippetJsonObject.getString("playlistId");
                    final String videoId = snippetJsonObject.getJSONObject("resourceId").getString("videoId");
                    YT_PLAYLIST_ID_VIDEO_ID_MAP.putIfAbsent(playlistId, videoId);
                });
    }
}
