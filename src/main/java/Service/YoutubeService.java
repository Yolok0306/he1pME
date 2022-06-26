package Service;

import Util.CommonUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class YoutubeService {
    protected void execute() {
        if (CommonUtil.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final String playListItemResponseString = callPlayListItemApi();
        if (StringUtils.isBlank(playListItemResponseString)) {
            return;
        }

        try {
            final JSONArray playListItemJsonArray = new JSONObject(playListItemResponseString).getJSONArray("items");
            final Map<String, Set<String>> videoIdMap = constructVideoIdMap(playListItemJsonArray);
            if (videoIdMap.isEmpty()) {
                return;
            }

            final String videoResponseString = callVideoApi(videoIdMap.keySet());
            if (StringUtils.isBlank(videoResponseString)) {
                return;
            }

            final JSONArray itemJsonArray = new JSONObject(videoResponseString).getJSONArray("items");
            for (final Object itemObject : itemJsonArray) {
                final JSONObject itemJsonObject = (JSONObject) itemObject;
                notification(itemJsonObject, videoIdMap);
            }
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private String callPlayListItemApi() {
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/playlistItems")
                    .addParameter("part", "contentDetails")
                    .addParameter("maxResults", "1")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY);
            CommonUtil.YOUTUBE_NOTIFICATION_MAP.keySet().forEach(id -> uriBuilder.addParameter("id", id));
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uriBuilder.build())
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, Set<String>> constructVideoIdMap(final JSONArray playListItemJsonArray) {
        if (playListItemJsonArray.isEmpty()) {
            return new HashMap<>();
        }

        final Map<String, Set<String>> videoIdMap = new HashMap<>();
        for (final Object playListItemObject : playListItemJsonArray) {
            final JSONObject playListItemJsonObject = (JSONObject) playListItemObject;
            final String startAtTime = playListItemJsonObject.getJSONObject("contentDetails").getString("videoPublishedAt");
            if (CommonUtil.checkStartAtTime(startAtTime)) {
                final String key = playListItemJsonObject.getJSONObject("contentDetails").getString("videoId");
                final Set<String> valueSet = CommonUtil.YOUTUBE_NOTIFICATION_MAP.get(playListItemJsonObject.getString("id"));
                videoIdMap.put(key, valueSet);
            }
        }
        return videoIdMap;
    }

    private String callVideoApi(final Set<String> videoIdSet) {
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/videos")
                    .addParameter("part", "snippet,liveStreamingDetails")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY);
            videoIdSet.forEach(videoId -> uriBuilder.addParameter("id", videoId));
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uriBuilder.build())
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void notification(final JSONObject videoJsonObject, final Map<String, Set<String>> videoIdMap) {
        final JSONObject snippetJsonObject = videoJsonObject.getJSONObject("snippet");
        final String videoId = videoJsonObject.getString("id");
        final String title = videoJsonObject.has("liveStreamingDetails") ? "開台通知" : "上片通知";
        final String desc = snippetJsonObject.getString("channelTitle") + " - " + snippetJsonObject.getString("title");
        final String thumb = snippetJsonObject.getJSONObject("thumbnails").getJSONObject("default").getString("url");
        final Color color = Color.of(192, 0, 0);
        final EmbedCreateFields.Author author = EmbedCreateFields.Author.of("Youtube", StringUtils.EMPTY, CommonUtil.YOUTUBE_LOGO_URI);

        for (final String messageChannelId : videoIdMap.get(videoId)) {
            CommonUtil.BOT.getChannelById(Snowflake.of(messageChannelId)).subscribe(channel -> {
                if (channel instanceof MessageChannel) {
                    final MessageChannel messageChannel = (MessageChannel) channel;
                    final EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc)
                            .thumbnail(thumb).color(color).author(author).build();
                    messageChannel.createMessage(embedCreateSpec).block();
                    messageChannel.createMessage("https://www.youtube.com/watch?v=" + videoId).block();
                }
            });
        }
    }
}
