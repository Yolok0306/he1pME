import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
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

public class He1pME extends ListenerAdapter {
    private static final Set<GatewayIntent> gatewayIntentSet;
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final TimerTaskService timerTaskService = new TimerTaskService();
    private static final Timer timer = new Timer();

    static {
        gatewayIntentSet = Set.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
    }

    public static void main(final String[] args) {
        final Properties properties = getProperties();
        CommonUtil.FREQUENCY = Long.parseLong(properties.getProperty("FREQUENCY"));
        CommonUtil.REGIONS = Regions.fromName(properties.getProperty("AWS_DEFAULT_REGION"));
        CommonUtil.BASIC_AWS_CREDENTIALS = new BasicAWSCredentials(
                properties.getProperty("AWS_ACCESS_KEY_ID"), properties.getProperty("AWS_SECRET_ACCESS_KEY")
        );
        CommonUtil.loadAllDataFromDB();
        initTwitchCache();
        initYoutubeCache();
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
            properties.load(new FileInputStream("src/main/resources/Config.properties"));
        } catch (final IOException exception) {
            throw new RuntimeException(exception.getMessage());
        }
        return properties;
    }

    private static void initTwitchCache() {
        if (TwitchService.TWITCH_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        TwitchService.addDataToTwitchCache(TwitchService.TWITCH_NOTIFICATION_MAP.keySet());
    }

    private static void initYoutubeCache() {
        if (YouTubeService.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return;
        }

        final Set<String> playlistItemResponseSet = YouTubeService.filterAndGetPlayListItemResponseSet(null);
        YouTubeService.addDataToYoutubeCache(playlistItemResponseSet);
    }
}
