package Service;

import SpecialDataStructure.UrlType;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CommonService {

    protected String getIdFromDB(final String searchValue) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("AllOfId").query(querySpec);

        String result = null;
        if (items.iterator().hasNext()) {
            result = items.iterator().next().getString("id");
        }

        dynamoDB.shutdown();
        return result;
    }

    protected String getUrlFromDB(final String searchValue, final UrlType type) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Url").query(querySpec);

        String result = null;
        if (items.iterator().hasNext() && StringUtils.equals(items.iterator().next().getString("type"), type.getType())) {
            result = items.iterator().next().getString("url");
        }

        dynamoDB.shutdown();
        return result;
    }

    protected void replyByHe1pMETemplate(final MessageCreateEvent event, final String title,
                                         final String desc, final String thumb) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Color color = Color.of(255, 192, 203);
        final EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder().title(title).description(desc)
                .thumbnail(Optional.ofNullable(thumb).orElse("")).color(color);
        event.getMember().ifPresent(member -> {
            final String name = member.getUsername();
            final String avatarUrl = member.getAvatarUrl();
            final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(name, null, avatarUrl);
            embedCreateSpec.author(author);
        });
        messageChannel.createMessage(embedCreateSpec.build()).block();
    }
}
