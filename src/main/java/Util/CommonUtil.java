package Util;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Optional;

@Slf4j
public class CommonUtil {
    public static final String SIGN = "$";
    public static String TOKEN;
    public static GatewayDiscordClient BOT;
    public static String BASE_URI = "https://discord.com/api/v9";

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

    public static void replyByHe1pMETemplate(final MessageCreateEvent event, final String title, final String desc, final String thumb) {
        event.getMessage().getChannel().subscribe(messageChannel -> {
            final String thumbnail = Optional.ofNullable(thumb).orElse(StringUtils.EMPTY);
            final Color color = Color.of(255, 192, 203);
            final EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                    .title(title).description(desc).thumbnail(thumbnail).color(color);
            event.getMember().ifPresent(member -> {
                final String name = member.getTag();
                final String avatarUrl = member.getAvatarUrl();
                final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(name, null, avatarUrl);
                embedCreateSpec.author(author);
            });
            messageChannel.createMessage(embedCreateSpec.build()).block();
        });
    }

    public static String descFormat(final String desc) {
        return StringUtils.abbreviate(desc, 43);
    }

    public static String descStartWithDiamondFormat(final String desc) {
        return StringUtils.abbreviate(desc, 36);
    }

    public static boolean isHigher(final Role role1, final Role role2) {
        return role1.getPosition().blockOptional().isPresent() && role2.getPosition().blockOptional().isPresent() &&
                role1.getPosition().blockOptional().get() > role2.getPosition().blockOptional().get();
    }
}
