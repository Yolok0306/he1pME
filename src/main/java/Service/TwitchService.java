package Service;

import Util.CommonUtil;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

@Slf4j
public class TwitchService {
    protected void execute() {
        final String responseString = callStreamApi();
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        try {
            final JSONObject responseJsonObject = new JSONObject(responseString);
            if (responseJsonObject.get("data") instanceof JSONArray) {
                final JSONArray jsonArray = (JSONArray) responseJsonObject.get("data");
                checkAndNotification(jsonArray);
            }
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private String callStreamApi() {
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.TWITCH_BASE_URI + "/streams");
            for (final String twitchChannel : CommonUtil.TWITCH_NOTIFICATION_MAP.keySet()) {
                uriBuilder.addParameter("user_login", twitchChannel);
            }
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uriBuilder.build())
                    .header("Client-Id", CommonUtil.CLIENT_ID)
                    .header("Authorization", CommonUtil.TOKEN_TYPE + StringUtils.SPACE + CommonUtil.ACCESS_TOKEN)
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

    private void checkAndNotification(final JSONArray jsonArray) {
        if (jsonArray.isEmpty()) {
            return;
        }

        for (final Object dataObject : jsonArray) {
            if (dataObject instanceof JSONObject) {
                final JSONObject dataJsonObject = (JSONObject) dataObject;
                final String type = dataJsonObject.get("type").toString();
                final ZonedDateTime startedAt = ZonedDateTime.parse(dataJsonObject.get("started_at").toString());
                final ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));
                if (StringUtils.equals(type, "live") && Duration.between(startedAt, now).toMillis() < CommonUtil.FREQUENCY) {
                    notification(dataJsonObject);
                }
            }
        }
    }

    private void notification(final JSONObject dataJsonObject) {
        final Set<String> messageChannelIdSet = CommonUtil.TWITCH_NOTIFICATION_MAP.get(dataJsonObject.getString("user_login"));
        final String title = "開台通知";
        final String desc = dataJsonObject.getString("user_name") + " - " + dataJsonObject.getString("title");
        final String thumb = handleThumb(dataJsonObject.getString("thumbnail_url"));
        final String uri = "https://www.twitch.tv/" + dataJsonObject.getString("user_login");
        for (final String messageChannelId : messageChannelIdSet) {
            CommonUtil.replyByTwitchTemplate(messageChannelId, title, desc, thumb, uri);
        }
    }

    private String handleThumb(final String thumb) {
        return thumb.replaceAll("-\\{width}x\\{height}", StringUtils.EMPTY);
    }
}
