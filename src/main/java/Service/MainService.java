package Service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MainService {
    protected final String IMAGE = "Image";
    protected final String YOUTUBE = "Youtube";
    protected final String TWITCH = "Twitch";
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());

    protected String getTokenFromDB(final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("PrivateInfo").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("token")));
        return result.toString();
    }

    protected String getUrlFromDB(final String tableName, final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable(tableName).query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("url")));
        return result.toString();
    }

    protected List<String> getActionFromDB(final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Action").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("action")));
        return Arrays.asList(result.toString().split(","));
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
        Optional.ofNullable(getTokenFromDB("Xun")).ifPresent(Xun -> {
            messageChannel.createMessage("<@" + Xun + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img)).block();
        });
    }

    protected void replyByXianTemplate(final MessageChannel messageChannel, final String msg, final String img) {
        Optional.ofNullable(getTokenFromDB("Xian")).ifPresent(Xian -> {
            messageChannel.createMessage("<@" + Xian + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(255, 222, 173)).setImage(img)).block();
        });
    }
}
