package Util;

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

import java.util.*;

@Slf4j
public class CommonUtil {
    public static final String SIGN = "$";
    public static String TOKEN;
    public static GatewayDiscordClient BOT;
    public static final Map<String, Set<String>> BAD_WORD_MAP = new HashMap<>();
    public static final String BASE_URI = "https://discord.com/api/v9";
    public static final Color HE1PME_COLOR = Color.of(255, 192, 203);

    public static void getBadWordFromDB() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("BadWord").scan(scanSpec);

        items.forEach(item -> BAD_WORD_MAP.compute(item.getString("guild_id"), (key, value) -> {
            final Set<String> badWordSet;
            if (Objects.isNull(value)) {
                badWordSet = new HashSet<>();
            } else {
                badWordSet = value;
            }
            badWordSet.add(item.getString("word"));
            return badWordSet;
        }));

        dynamoDB.shutdown();
    }

    public static Optional<Item> getMemberDataFromDB(final String name, final String guildId) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
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
