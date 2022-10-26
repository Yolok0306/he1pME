import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import service.MessageEventService;
import service.TimerTaskService;
import service.YouTubeService;
import util.CommonUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class He1pME extends ListenerAdapter {
    private static final Set<GatewayIntent> gatewayIntentSet = Set.of(GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final TimerTaskService timerTaskService = new TimerTaskService();
    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        final Properties properties = getProperties();
        CommonUtil.FREQUENCY = Long.parseLong(properties.getProperty("FREQUENCY"));
        CommonUtil.REGIONS = Regions.fromName(properties.getProperty("AWS_DEFAULT_REGION"));
        CommonUtil.BASIC_AWS_CREDENTIALS = new BasicAWSCredentials(
                properties.getProperty("AWS_ACCESS_KEY_ID"), properties.getProperty("AWS_SECRET_ACCESS_KEY")
        );
        CommonUtil.loadAllDataFromDB();
        YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP = constructYTPlaylistIdVideoIdMap();
        CommonUtil.JDA = JDABuilder.createDefault(properties.getProperty("DISCORD_BOT_TOKEN"))
                .enableIntents(gatewayIntentSet).addEventListeners(new He1pME()).build();
        timer.schedule(timerTaskService, 0, CommonUtil.FREQUENCY);
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        final MessageChannel messageChannel = event.getMessage().getChannel();
        final Member member = event.getMember();
        final Message message = event.getMessage();
        messageEventService.execute(messageChannel, member, message);
    }

    @Override
    public void onMessageUpdate(@NotNull final MessageUpdateEvent event) {
        final MessageChannel messageChannel = event.getMessage().getChannel();
        final Message message = event.getMessage();
        final Member member = event.getMember();
        messageEventService.execute(messageChannel, member, message);
    }

    private static Properties getProperties() {
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("src/main/resources/DynamoDB.properties"));
        } catch (final IOException exception) {
            throw new RuntimeException(exception.getMessage());
        }
        return properties;
    }

    private static Map<String, String> constructYTPlaylistIdVideoIdMap() {
        if (YouTubeService.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return new HashMap<>();
        }

        final Set<String> playlistItemResponseSet = YouTubeService.YOUTUBE_NOTIFICATION_MAP.keySet().stream()
                .map(YouTubeService::callPlayListItemApi).collect(Collectors.toSet());
        return playlistItemResponseSet.stream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .filter(snippetJsonObject -> !CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt"), null))
                .collect(Collectors.toMap(snippetJsonObject -> snippetJsonObject.getString("playlistId"),
                        snippetJsonObject -> snippetJsonObject.getJSONObject("resourceId").getString("videoId"),
                        (existing, replacement) -> existing, HashMap::new));
    }
}
