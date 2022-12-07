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
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TwitchService implements Runnable {
    public static final Map<String, String> TWITCH_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, Set<String>> TWITCH_NOTIFICATION_MAP = new ConcurrentHashMap<>();

    @Override
    public void run() {
        if (TWITCH_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final String responseString = callStreamApi(TWITCH_NOTIFICATION_MAP.keySet());
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        try {
            final JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
            if (dataJsonArray.isEmpty()) {
                return;
            }

            dataJsonArray.toList().parallelStream()
                    .filter(data -> data instanceof HashMap<?, ?>)
                    .map(data -> (Map<?, ?>) data)
                    .forEach(data -> {
                        final String type = data.get("type").toString();
                        if (!StringUtils.equals(type, "live")) {
                            return;
                        }

                        final String userLogin = data.get("user_login").toString();
                        final String id = data.get("id").toString();
                        if (!TWITCH_CACHE.containsKey(userLogin) || !StringUtils.equals(TWITCH_CACHE.get(userLogin), id)) {
                            TWITCH_CACHE.put(userLogin, id);
                            notification(data);
                        }
                    });
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private static String callStreamApi(final Set<String> userLoginSet) {
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final URIBuilder uriBuilder = new URIBuilder(CommonUtil.TWITCH_API_BASE_URI + "/streams");
            userLoginSet.parallelStream().forEach(key -> uriBuilder.addParameter("user_login", key));
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

    private void notification(final Map<?, ?> data) {
        final String userLogin = data.get("user_login").toString();
        final String title = data.get("user_name").toString();
        final String desc = data.get("title").toString();
        final String thumb = data.get("thumbnail_url").toString().replace("-{width}x{height}", StringUtils.EMPTY);
        final Color color = new Color(144, 0, 255);

        for (final String messageChannelId : TWITCH_NOTIFICATION_MAP.get(userLogin)) {
            final MessageChannel messageChannel = CommonUtil.JDA.getChannelById(MessageChannel.class, messageChannelId);
            if (messageChannel == null) {
                continue;
            }

            final MessageEmbed messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb)
                    .setColor(color).setAuthor("Twitch", null, CommonUtil.TWITCH_LOGO_URI).build();
            messageChannel.sendMessage("https://www.twitch.tv/" + userLogin).addEmbeds(messageEmbed).queue();
        }
    }

    public static void addDataToTwitchCache(final Set<String> userLoginSet) {
        final String responseString = TwitchService.callStreamApi(userLoginSet);
        if (StringUtils.isBlank(responseString)) {
            return;
        }

        try {
            final JSONArray dataJsonArray = new JSONObject(responseString).getJSONArray("data");
            if (dataJsonArray.isEmpty()) {
                return;
            }

            dataJsonArray.toList().parallelStream()
                    .filter(data -> data instanceof HashMap<?, ?>)
                    .map(data -> (Map<?, ?>) data)
                    .forEach(data -> {
                        final String type = data.get("type").toString();
                        final String startedAt = data.get("started_at").toString();
                        if (StringUtils.equals(type, "live") && CommonUtil.checkStartTime(startedAt)) {
                            final String userLogin = data.get("user_login").toString();
                            final String id = data.get("id").toString();
                            TWITCH_CACHE.put(userLogin, id);
                        }
                    });
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }
}
