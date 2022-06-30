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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class YoutubeService {
    protected void execute() {
        if (CommonUtil.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final Set<String> playlistItemResponseSet = CommonUtil.YOUTUBE_NOTIFICATION_MAP.keySet().stream()
                .map(this::callPlayListItemApi).collect(Collectors.toSet());

        try {
            final Map<String, Set<String>> videoIdMap = constructVideoIdMap(playlistItemResponseSet);
            if (videoIdMap.isEmpty()) {
                return;
            }

            final String videoResponseString = callVideoApi(videoIdMap.keySet());
            if (StringUtils.isBlank(videoResponseString)) {
                return;
            }

            final JSONArray itemJsonArray = new JSONObject(videoResponseString).getJSONArray("items");
            for (int i = 0; i < itemJsonArray.length(); i++) {
                notification(itemJsonArray.getJSONObject(i), videoIdMap);
            }
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private String callPlayListItemApi(final String playlistId) {
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMillis(1000)).build();
            final URI uri = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/playlistItems")
                    .addParameter("playlistId", playlistId)
                    .addParameter("part", "snippet")
                    .addParameter("maxResults", "1")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY)
                    .build();
            final HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(uri).build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private Map<String, Set<String>> constructVideoIdMap(final Set<String> playlistItemResponseSet) {
        return playlistItemResponseSet.stream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0))
                .map(firstPlaylistItemJsonObject -> firstPlaylistItemJsonObject.getJSONObject("snippet"))
                .filter(snippetJsonObject -> CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt")))
                .collect(Collectors.toMap(snippetJsonObject -> snippetJsonObject.getJSONObject("resourceId").getString("videoId"),
                        snippetJsonObject -> CommonUtil.YOUTUBE_NOTIFICATION_MAP.get(snippetJsonObject.getString("playlistId")),
                        (existing, replacement) -> existing, HashMap::new));
    }

    private String callVideoApi(final Set<String> videoIdSet) {
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMillis(1000)).build();
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.YOUTUBE_API_BASE_URI + "/videos")
                    .addParameter("part", "snippet,liveStreamingDetails")
                    .addParameter("key", CommonUtil.YOUTUBE_API_KEY);
            videoIdSet.forEach(videoId -> uriBuilder.addParameter("id", videoId));
            final HttpRequest httpRequest = HttpRequest.newBuilder().GET().uri(uriBuilder.build()).build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private void notification(final JSONObject videoJsonObject, final Map<String, Set<String>> videoIdMap) {
        if (videoJsonObject.has("liveStreamingDetails") && videoJsonObject.getJSONObject("liveStreamingDetails").has("actualEndTime")) {
            return;
        }

        final JSONObject snippetJsonObject = videoJsonObject.getJSONObject("snippet");
        final String videoId = videoJsonObject.getString("id");
        final String title = videoJsonObject.has("liveStreamingDetails") ? "開台通知" : "上片通知";
        final String desc = snippetJsonObject.getString("channelTitle") + " - " + snippetJsonObject.getString("title");
        final String thumb = getThumbnail(snippetJsonObject.getJSONObject("thumbnails"));
        final Color color = Color.of(255, 0, 0);
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
}
