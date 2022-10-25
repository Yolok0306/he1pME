package util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import plugin.GuildAudioManager;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
public class CommonUtil {
    public static JDA JDA;
    public static String SIGN;
    public static long FREQUENCY;
    public static Regions REGIONS;
    public static BasicAWSCredentials BASIC_AWS_CREDENTIALS;
    public static String TWITCH_API_CLIENT_ID;
    public static String TWITCH_API_TOKEN_TYPE;
    public static String TWITCH_API_ACCESS_TOKEN;
    public static String TWITCH_API_BASE_URI;
    public static String TWITCH_LOGO_URI;
    public static String YOUTUBE_API_KEY;
    public static String YOUTUBE_API_BASE_URI;
    public static String YOUTUBE_LOGO_URI;
    public static final Color HE1PME_COLOR = new Color(255, 192, 203);
    public static final Map<Long, GuildAudioManager> AUDIO_MANAGER_MAP = new HashMap<>();
    public static final Map<String, Set<String>> BAD_WORD_MAP = new HashMap<>();
    public static final Map<String, Set<String>> TWITCH_NOTIFICATION_MAP = new HashMap<>();
    public static final Map<String, Set<String>> YOUTUBE_NOTIFICATION_MAP = new HashMap<>();
    public static Map<String, String> YT_PLAYLIST_ID_VIDEO_ID_MAP;

    public static void loadAllDataFromDB() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final ScanSpec scanSpec = new ScanSpec();
        getServerDataFromDB(dynamoDB, scanSpec);
        getBadWordFromDB(dynamoDB, scanSpec);
        getTwitchNotificationFromDB(dynamoDB, scanSpec);
        getYouTubeNotificationFromDB(dynamoDB, scanSpec);
        dynamoDB.shutdown();
    }

    private static void getServerDataFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("ServerData").scan(scanSpec);

        if (!items.iterator().hasNext()) {
            throw new IllegalStateException("Can not get Token from ServerData table!");
        }

        items.forEach(item -> {
            switch (item.getString("name")) {
                case "Sign":
                    SIGN = item.getString("id");
                    break;

                case "Twitch Api Client Id":
                    TWITCH_API_CLIENT_ID = item.getString("id");
                    break;

                case "Twitch Api Token Type":
                    TWITCH_API_TOKEN_TYPE = item.getString("id");
                    break;

                case "Twitch Api Access Token":
                    TWITCH_API_ACCESS_TOKEN = item.getString("id");
                    break;

                case "Twitch Api Base Uri":
                    TWITCH_API_BASE_URI = item.getString("id");
                    break;

                case "Twitch Logo Uri":
                    TWITCH_LOGO_URI = item.getString("id");
                    break;

                case "Youtube Api Key":
                    YOUTUBE_API_KEY = item.getString("id");
                    break;

                case "Youtube Api Base Uri":
                    YOUTUBE_API_BASE_URI = item.getString("id");
                    break;

                case "Youtube Logo Uri":
                    YOUTUBE_LOGO_URI = item.getString("id");
                    break;
            }
        });
    }

    private static void getBadWordFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("BadWord").scan(scanSpec);

        items.forEach(item -> BAD_WORD_MAP.compute(item.getString("guild_id"), (key, value) -> {
            final Set<String> innerSet = value != null ? value : new HashSet<>();
            innerSet.add(item.getString("word"));
            return innerSet;
        }));
    }

    private static void getTwitchNotificationFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("TwitchNotification").scan(scanSpec);

        items.forEach(item -> TWITCH_NOTIFICATION_MAP.compute(item.getString("twitch_channel_id"), (key, value) -> {
            final Set<String> innerSet = value != null ? value : new HashSet<>();
            innerSet.add(item.getString("message_channel_id"));
            return innerSet;
        }));
    }

    private static void getYouTubeNotificationFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("YouTubeNotification").scan(scanSpec);

        items.forEach(item -> YOUTUBE_NOTIFICATION_MAP.compute(item.getString("youtube_channel_playlist_id"), (key, value) -> {
            final Set<String> innerSet = value != null ? value : new HashSet<>();
            innerSet.add(item.getString("message_channel_id"));
            return innerSet;
        }));
    }

    public static synchronized GuildAudioManager getGuildAudioPlayer(final Guild guild, final AudioPlayerManager playerManager) {
        final long guildId = Long.parseLong(guild.getId());
        AUDIO_MANAGER_MAP.computeIfAbsent(guildId, (key) -> new GuildAudioManager(playerManager, guild));
        final GuildAudioManager audioManager = AUDIO_MANAGER_MAP.get(guildId);
        guild.getAudioManager().setSendingHandler(audioManager.getSendHandler());
        return audioManager;
    }

    public static boolean checkStartTime(final String startTimeString, final ZonedDateTime now) {
        final ZonedDateTime startTime = ZonedDateTime.parse(startTimeString);
        final ZonedDateTime nowAfterCheck = Optional.ofNullable(now).orElse(ZonedDateTime.now(ZoneId.of("UTC")));
        return Duration.between(startTime, nowAfterCheck).toSeconds() < Duration.ofMillis(FREQUENCY).toSeconds();
    }

    public static Optional<Item> getMemberDataFromDB(final String name, final String guildId) {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key1 = :value1 and #key2 = :value2")
                .withNameMap(new NameMap().with("#key1", "name").with("#key2", "guild_id"))
                .withValueMap(new ValueMap().withString(":value1", name).withString(":value2", guildId));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("MemberData").query(querySpec);

        final Item result;
        if (items.iterator().hasNext()) {
            result = items.iterator().next();
        } else {
            result = null;
            log.error("Can not find data with name = \"" + name + "\" and guild_id = \"" + guildId + "\" in MemberData Table!");
        }

        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    public static void replyByHe1pMETemplate(final MessageChannel messageChannel, final Member member,
                                             final String title, final String desc, final String thumb) {
        final MessageEmbed messageEmbed;
        if (StringUtils.isNotBlank(thumb)) {
            messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setThumbnail(thumb).setColor(HE1PME_COLOR)
                    .setAuthor(member.getUser().getAsTag(), null, getRealAvatarUrl(member)).build();
        } else {
            messageEmbed = new EmbedBuilder().setTitle(title).setDescription(desc).setColor(HE1PME_COLOR)
                    .setAuthor(member.getUser().getAsTag(), null, getRealAvatarUrl(member)).build();
        }
        messageChannel.sendMessageEmbeds(messageEmbed).queue();
    }

    public static String descFormat(final String desc) {
        return StringUtils.abbreviate(desc, 43);
    }

    public static String descStartWithDiamondFormat(final String desc) {
        return StringUtils.abbreviate(desc, 36);
    }

    public static String getRealAvatarUrl(final Member member) {
        return Optional.ofNullable(member.getUser().getAvatarUrl()).orElse(member.getUser().getDefaultAvatarUrl());
    }
}
