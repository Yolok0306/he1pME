import Service.MessageEventService;
import Service.TimerTaskService;
import Service.VoiceStateService;
import Service.YoutubeService;
import Util.CommonUtil;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class He1pME {
    private static final IntentSet intentSet = IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES,
            Intent.GUILD_VOICE_STATES);
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final VoiceStateService voiceStateService = new VoiceStateService();
    private static final TimerTaskService timerTaskService = new TimerTaskService();
    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        setDynamoDBConfig();
        CommonUtil.getServerDataFromDB();
        CommonUtil.getBadWordFromDB();
        CommonUtil.getTwitchNotificationFromDB();
        CommonUtil.getYouTubeNotificationFromDB();

        CommonUtil.BOT = DiscordClient.create(CommonUtil.DISCORD_API_TOKEN).gateway().setEnabledIntents(intentSet).login()
                .blockOptional().orElseThrow(() -> new IllegalStateException("Bot token : " + CommonUtil.DISCORD_API_TOKEN + " is invalid !"));
        CommonUtil.YT_PLAYLIST_ID_VIDEO_ID_MAP = constructYTPlaylistIdVideoIdMap();

        CommonUtil.BOT.getEventDispatcher().on(ReadyEvent.class).subscribe(event ->
                System.out.printf("-----Logged in as %s #%s-----%n", event.getSelf().getUsername(), event.getSelf().getDiscriminator()));

        timer.schedule(timerTaskService, 0, CommonUtil.FREQUENCY);
        CommonUtil.BOT.getEventDispatcher().on(MessageCreateEvent.class).subscribe(messageEventService::receiveEvent);
        CommonUtil.BOT.getEventDispatcher().on(MessageUpdateEvent.class).subscribe(messageEventService::receiveEvent);
        CommonUtil.BOT.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(voiceStateService::receiveEvent);
        CommonUtil.BOT.onDisconnect().block();
    }

    public static void setDynamoDBConfig() {
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("src/resources/DynamoDB.properties"));
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        CommonUtil.REGIONS = Regions.fromName(properties.getProperty("AWS_DEFAULT_REGION"));
        CommonUtil.BASIC_AWS_CREDENTIALS = new BasicAWSCredentials(
                properties.getProperty("AWS_ACCESS_KEY_ID"), properties.getProperty("AWS_SECRET_ACCESS_KEY")
        );
    }

    private static Map<String, String> constructYTPlaylistIdVideoIdMap() {
        if (CommonUtil.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            return new HashMap<>();
        }

        final Set<String> playlistItemResponseSet = CommonUtil.YOUTUBE_NOTIFICATION_MAP.keySet().stream()
                .map(YoutubeService::callPlayListItemApi).collect(Collectors.toSet());
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
