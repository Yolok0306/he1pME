package Util;

import SpecialDataStructure.UrlType;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class CommonUtil {
    public static final String SIGN = "$";
    public static final Snowflake muteRole = Snowflake.of(CommonUtil.getIdFromDB("MuteRole").orElse(StringUtils.EMPTY));

    public static Optional<String> getIdFromDB(final String searchValue) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("AllOfId").query(querySpec);

        if (!items.iterator().hasNext()) {
            log.error(searchValue + "'s id is not in database !");
            return Optional.empty();
        }

        final String result = items.iterator().next().getString("id");
        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    public static Optional<String> getUrlFromDB(final String searchValue, final UrlType type) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Url").query(querySpec);

        if (!items.iterator().hasNext()) {
            log.error(searchValue + "'s url is not in database !");
            return Optional.empty();
        }

        final Item item = items.iterator().next();

        if (!StringUtils.equals(item.getString("type"), type.getType())) {
            log.error(searchValue + "'s url type is not equal to \"" + type.getType() + "\" !");
            return Optional.empty();
        }

        final String result = item.getString("url");
        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    public static void replyByHe1pMETemplate(final MessageCreateEvent event, final String title,
                                             final String desc, final String thumb) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Color color = Color.of(255, 192, 203);
        final EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc)
                .thumbnail(Optional.ofNullable(thumb).orElse(StringUtils.EMPTY)).color(color);
        event.getMember().ifPresent(member -> {
            final String name = member.getTag();
            final String avatarUrl = member.getAvatarUrl();
            final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(name, null, avatarUrl);
            embedCreateSpec.author(author);
        });
        messageChannel.createMessage(embedCreateSpec.build()).block();
    }

    public static String descFormat(final String desc) {
        return StringUtils.abbreviate(desc,43);
    }

    public static String descStartWithDiamondFormat(final String desc) {
        return StringUtils.abbreviate(desc,36);
    }

    public static boolean isHigher(final Role role1, final Role role2) {
        return role1.getPosition().blockOptional().isPresent() && role2.getPosition().blockOptional().isPresent() &&
                role1.getPosition().blockOptional().get() > role2.getPosition().blockOptional().get();
    }
}
