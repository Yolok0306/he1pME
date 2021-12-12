package Service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import org.apache.commons.codec.binary.StringUtils;

import java.util.*;

public class CommonService {
    protected final String IMAGE = "Image";
    protected final String YOUTUBE = "Youtube";
    protected final String TWITCH = "Twitch";
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());

    protected List<String> getAllowChatRoom() {
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", "AllowChatRoom");
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("AllOfId").query(querySpec);
        final List<String> result = new ArrayList<>();
        items.forEach(item -> result.add(item.getString("id")));
        return result;
    }

    protected String getIdFromDB(final String searchValue) {
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("AllOfId").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("id")));
        return result.toString();
    }

    protected String getUrlFromDB(final String searchValue, final String type) {
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Url").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> {
            if (StringUtils.equals(item.getString("type"), type)) {
                result.append(item.getString("url"));
            }
        });
        return result.toString();
    }

    protected void replyByHe1pMETemplate(final MessageChannel messageChannel, final String msg) {
        messageChannel.createEmbed(spec -> {
            spec.setTitle(msg);
            spec.setColor(Color.of(255, 192, 203));
        }).block();
    }

    protected void replyByDefaultTemplate(final MessageChannel messageChannel, final String id, final String msg, final String img) {
        messageChannel.createMessage("<@" + id + "> " + msg).block();
        messageChannel.createEmbed(spec -> spec.setColor(Color.BLUE).setImage(img)).block();
    }

    protected void replyByXunTemplate(final MessageChannel messageChannel, final String msg, final String img) {
        Optional.ofNullable(getIdFromDB("Xun")).ifPresent(Xun -> {
            messageChannel.createMessage("<@" + Xun + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img)).block();
        });
    }

    protected void replyByXianTemplate(final MessageChannel messageChannel, final String msg, final String img) {
        Optional.ofNullable(getIdFromDB("Xian")).ifPresent(Xian -> {
            messageChannel.createMessage("<@" + Xian + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(255, 222, 173)).setImage(img)).block();
        });
    }
}
