package Util;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class CommonUtil {
    public static final String SIGN = "$";
    public static String TOKEN;
    public static GatewayDiscordClient BOT;
    public static final Set<String> BAD_WORD_SET = new HashSet<>();
    public static String BASE_URI = "https://discord.com/api/v9";

    public static void loadServerDataFromDB() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("ServerData").scan(scanSpec);

        if (!items.iterator().hasNext()) {
            throw new IllegalStateException("Can not get any data from ServerData table !");
        }

        for (final Item item : items) {
            if (StringUtils.equals(item.getString("name"), "Token")) {
                TOKEN = item.getString("id");
            } else if (StringUtils.equals(item.getString("name"), "BadWord")) {
                BAD_WORD_SET.add(item.getString("id"));
            }
        }
        dynamoDB.shutdown();
    }

    public static Optional<Item> getMemberDataFromDB(final String searchValue) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key = :value")
                .withNameMap(Collections.singletonMap("#key", "name"))
                .withValueMap(Collections.singletonMap(":value", searchValue));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("MemberData").query(querySpec);

        final Item result;
        if (items.iterator().hasNext()) {
            result = items.iterator().next();
        } else {
            result = null;
            log.error("Can not find data with name = \"" + searchValue + "\" in MemberData Table !");
        }

        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    public static void replyByHe1pMETemplate(final MessageChannel messageChannel, final Member member,
                                             final String title, final String desc, final String thumb) {
        final String thumbnail = Optional.ofNullable(thumb).orElse(StringUtils.EMPTY);
        final Color color = Color.of(255, 192, 203);
        final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(member.getTag(), null, member.getAvatarUrl());
        final EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder()
                .title(title).description(desc).thumbnail(thumbnail).color(color).author(author).build();
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
