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
import java.time.Duration;
import java.time.ZonedDateTime;

@Slf4j
public class TwitchService {
    protected void execute(final ZonedDateTime now) {
        if (CommonUtil.TWITCH_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final String responseString = callStreamApi();
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        try {
            final JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
            if (dataJsonArray.isEmpty()) {
                return;
            }

            for (int i = 0; i < dataJsonArray.length(); i++) {
                final String type = dataJsonArray.getJSONObject(i).getString("type");
                final String startedAt = dataJsonArray.getJSONObject(i).getString("started_at");
                if (StringUtils.equals(type, "live") && CommonUtil.checkStartTime(startedAt, now)) {
                    notification(dataJsonArray.getJSONObject(i));
                }
            }
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private String callStreamApi() {
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMillis(1000)).build();
        try {
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.TWITCH_API_BASE_URI + "/streams");
            CommonUtil.TWITCH_NOTIFICATION_MAP.keySet().forEach(key -> uriBuilder.addParameter("user_login", key));
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(uriBuilder.build())
                    .header("Client-Id", CommonUtil.TWITCH_API_CLIENT_ID)
                    .header("Authorization", CommonUtil.TWITCH_API_TOKEN_TYPE + StringUtils.SPACE + CommonUtil.TWITCH_API_ACCESS_TOKEN)
                    .build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.body();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    private void notification(final JSONObject dataJsonObject) {
        final String userLogin = dataJsonObject.getString("user_login");
        final String title = "????????????";
        final String desc = dataJsonObject.getString("user_name") + " - " + dataJsonObject.getString("title");
        final String thumb = dataJsonObject.getString("thumbnail_url").replace("-{width}x{height}", StringUtils.EMPTY);
        final Color color = Color.of(144, 0, 255);
        final EmbedCreateFields.Author author = EmbedCreateFields.Author.of("Twitch", StringUtils.EMPTY, CommonUtil.TWITCH_LOGO_URI);

        for (final String messageChannelId : CommonUtil.TWITCH_NOTIFICATION_MAP.get(userLogin)) {
            CommonUtil.BOT.getChannelById(Snowflake.of(messageChannelId)).subscribe(channel -> {
                if (channel instanceof MessageChannel) {
                    final MessageChannel messageChannel = (MessageChannel) channel;
                    final EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc)
                            .thumbnail(thumb).color(color).author(author).build();
                    messageChannel.createMessage(embedCreateSpec).block();
                    messageChannel.createMessage("https://www.twitch.tv/" + userLogin).block();
                }
            });
        }
    }
}
