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
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import service.GoodBoyService;
import service.TwitchService;
import service.YouTubeService;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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
            throw new IllegalStateException("Could not get any data in ServerData Table!");
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
        if (!items.iterator().hasNext()) {
            log.error("Could not get any data in BadWord Table!");
            return;
        }

        items.forEach(item -> {
            final String key = item.getString("guild_id");
            GoodBoyService.BAD_WORD_MAP.computeIfAbsent(key, (k) -> new HashSet<>());
            final Set<String> value = GoodBoyService.BAD_WORD_MAP.get(key);
            value.add(item.getString("word"));
        });
    }

    private static void getTwitchNotificationFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("TwitchNotification").scan(scanSpec);
        if (!items.iterator().hasNext()) {
            log.error("Could not get any data in TwitchNotification Table!");
            return;
        }

        items.forEach(item -> {
            final String key = item.getString("twitch_channel_id");
            TwitchService.TWITCH_NOTIFICATION_MAP.computeIfAbsent(key, (k) -> new HashSet<>());
            final Set<String> value = TwitchService.TWITCH_NOTIFICATION_MAP.get(key);
            value.add(item.getString("message_channel_id"));
        });
    }

    private static void getYouTubeNotificationFromDB(final DynamoDB dynamoDB, final ScanSpec scanSpec) {
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("YouTubeNotification").scan(scanSpec);
        if (!items.iterator().hasNext()) {
            log.error("Could not get any data in YouTubeNotification Table!");
            return;
        }

        items.forEach(item -> {
            final String key = item.getString("youtube_channel_playlist_id");
            YouTubeService.YOUTUBE_NOTIFICATION_MAP.computeIfAbsent(key, (k) -> new HashSet<>());
            final Set<String> value = YouTubeService.YOUTUBE_NOTIFICATION_MAP.get(key);
            value.add(item.getString("message_channel_id"));
        });
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
            log.error("Unable to get data for name = {} and guild_id = {} in MemberData Table!", name, guildId);
        }

        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    public static void replyByHe1pMETemplate(final MessageChannel messageChannel, final Member member,
                                             final String title, final String desc, final String thumb) {
        final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(title).setDescription(desc).setColor(HE1PME_COLOR)
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        if (StringUtils.isNotBlank(thumb)) {
            embedBuilder.setThumbnail(thumb);
        }
        messageChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public static boolean checkStartTime(final String startTimeString) {
        final ZonedDateTime startTime = ZonedDateTime.parse(startTimeString);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        return Duration.between(startTime, now).toSeconds() >= Duration.ofMillis(FREQUENCY).toSeconds();
    }

    public static String descFormat(final String desc) {
        return StringUtils.abbreviate(desc, 43);
    }

    public static String descStartWithDiamondFormat(final String desc) {
        return StringUtils.abbreviate(desc, 36);
    }

    public static boolean notHigher(final Member source, final Member target) {
        if (CollectionUtils.isEmpty(source.getRoles()) || CollectionUtils.isEmpty(target.getRoles())) {
            return true;
        }

        final List<Integer> sourcePositionList = source.getRoles().stream()
                .map(Role::getPosition)
                .sorted(Comparator.comparing(Integer::intValue).reversed())
                .collect(Collectors.toList());
        final List<Integer> targetPositionList = target.getRoles().stream()
                .map(Role::getPosition)
                .sorted(Comparator.comparing(Integer::intValue).reversed())
                .collect(Collectors.toList());
        return sourcePositionList.get(0) <= targetPositionList.get(0) || target.getPermissions().contains(Permission.ADMINISTRATOR);
    }

    public static boolean notHigher(final Member member, final Role role) {
        if (CollectionUtils.isEmpty(member.getRoles())) {
            return true;
        }

        return member.getRoles().stream().noneMatch(eachRole -> eachRole.getPosition() > role.getPosition());
    }
}
