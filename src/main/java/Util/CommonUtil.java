package Util;

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
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
public class CommonUtil {
    public static final String SIGN = "$";
    public static final long FREQUENCY = 300000;
    public static Regions REGIONS;
    public static BasicAWSCredentials BASIC_AWS_CREDENTIALS;
    public static String DISCORD_API_TOKEN;
    public static String DISCORD_API_TOKEN_TYPE;
    public static String DISCORD_API_BASE_URI;
    public static String TWITCH_API_CLIENT_ID;
    public static String TWITCH_API_TOKEN_TYPE;
    public static String TWITCH_API_ACCESS_TOKEN;
    public static String TWITCH_API_BASE_URI;
    public static String TWITCH_LOGO_URI;
    public static String YOUTUBE_API_KEY;
    public static String YOUTUBE_API_BASE_URI;
    public static String YOUTUBE_LOGO_URI;
    public static final Color HE1PME_COLOR = Color.of(255, 192, 203);
    public static final Map<String, Set<String>> BAD_WORD_MAP = new HashMap<>();
    public static final Map<String, Set<String>> TWITCH_NOTIFICATION_MAP = new HashMap<>();
    public static final Map<String, Set<String>> YOUTUBE_NOTIFICATION_MAP = new HashMap<>();
    public static GatewayDiscordClient BOT;
    public static Map<String, String> YT_PLAYLIST_ID_VIDEO_ID_MAP;

    public static void getServerDataFromDB() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("ServerData").scan(scanSpec);

        if (!items.iterator().hasNext()) {
            throw new IllegalStateException("Can not get Token from ServerData table!");
        }

        items.forEach(item -> {
            switch (item.getString("name")) {
                case "Discord Api Token":
                    DISCORD_API_TOKEN = item.getString("id");
                    break;

                case "Discord Api Token Type":
                    DISCORD_API_TOKEN_TYPE = item.getString("id");
                    break;

                case "Discord Api Base Uri":
                    DISCORD_API_BASE_URI = item.getString("id");
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

        dynamoDB.shutdown();
    }

    public static void getBadWordFromDB() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("BadWord").scan(scanSpec);

        items.forEach(item -> BAD_WORD_MAP.compute(item.getString("guild_id"), (key, value) -> {
            final Set<String> innerSet = Objects.nonNull(value) ? value : new HashSet<>();
            innerSet.add(item.getString("word"));
            return innerSet;
        }));

        dynamoDB.shutdown();
    }

    public static void getTwitchNotificationFromDB() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("TwitchNotification").scan(scanSpec);

        items.forEach(item -> TWITCH_NOTIFICATION_MAP.compute(item.getString("twitch_channel_id"), (key, value) -> {
            final Set<String> innerSet = Objects.nonNull(value) ? value : new HashSet<>();
            innerSet.add(item.getString("message_channel_id"));
            return innerSet;
        }));

        dynamoDB.shutdown();
    }

    public static void getYouTubeNotificationFromDB() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("YouTubeNotification").scan(scanSpec);

        items.forEach(item -> YOUTUBE_NOTIFICATION_MAP.compute(item.getString("youtube_channel_playlist_id"), (key, value) -> {
            final Set<String> innerSet = Objects.nonNull(value) ? value : new HashSet<>();
            innerSet.add(item.getString("message_channel_id"));
            return innerSet;
        }));

        dynamoDB.shutdown();
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
        final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(member.getTag(), StringUtils.EMPTY, member.getAvatarUrl());
        final EmbedCreateSpec embedCreateSpec;
        if (StringUtils.isNotBlank(thumb)) {
            embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc).thumbnail(thumb).color(HE1PME_COLOR).author(author).build();
        } else {
            embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc).color(HE1PME_COLOR).author(author).build();
        }
        messageChannel.createMessage(embedCreateSpec).block();
    }

    public static String descFormat(final String desc) {
        return StringUtils.abbreviate(desc, 43);
    }

    public static String descStartWithDiamondFormat(final String desc) {
        return StringUtils.abbreviate(desc, 36);
    }

    public static boolean isNotHigher(final Role role1, final Role role2) {
        return role1.getPosition().blockOptional().isEmpty() || role2.getPosition().blockOptional().isEmpty() ||
                role1.getPosition().blockOptional().get() <= role2.getPosition().blockOptional().get();
    }
}
