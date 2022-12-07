import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import service.MessageEventService;
import service.TimerTaskService;
import service.TwitchService;
import service.YouTubeService;
import util.CommonUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.stream.Collectors;

public class He1pME extends ListenerAdapter {
    private static final Set<GatewayIntent> gatewayIntentSet = Set.of(GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
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
        addDataToTwitchCache();
        addDataToYoutubeCache();
        CommonUtil.JDA = JDABuilder.createDefault(properties.getProperty("DISCORD_BOT_TOKEN"), gatewayIntentSet)
                .addEventListeners(new He1pME()).build();
        timer.schedule(timerTaskService, 5000, CommonUtil.FREQUENCY);
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        messageEventService.execute(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull final MessageUpdateEvent event) {
        messageEventService.execute(event.getMessage());
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

    private static void addDataToTwitchCache() {
        if (TwitchService.TWITCH_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        try {
            TwitchService.addDataToTwitchCache(TwitchService.TWITCH_NOTIFICATION_MAP.keySet());
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private static void addDataToYoutubeCache() {
        if (YouTubeService.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final Set<String> playlistItemResponseSet = YouTubeService.YOUTUBE_NOTIFICATION_MAP.keySet().parallelStream()
                .map(YouTubeService::callPlayListItemApi).collect(Collectors.toSet());
        try {
            YouTubeService.addDataToYoutubeCache(playlistItemResponseSet);
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }
}
