package Service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MainService {
    private final String SIGN = "/";
    protected final String IMAGE = "Image";
    protected final String YOUTUBE = "Youtube";
    protected final String TWITCH = "Twitch";
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());

    protected Boolean checkSign(final String content) {
        return content.startsWith(SIGN);
    }

    protected String format(final String content) {
        final String instruction = content.split(" ")[0];
        return new StringBuilder(instruction).delete(0, SIGN.length()).toString();
    }

    protected Optional<String> getTokenFromDB(final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("PrivateInfo").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("token")));
        return Optional.of(result.toString());
    }

    protected Optional<String> getUrlFromDB(final String tableName, final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable(tableName).query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("url")));
        return Optional.of(result.toString());
    }

    protected Optional<List<String>> getActionFromDB(final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Action").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("action")));
        return Optional.of(Arrays.asList(result.toString().split(",")));
    }
}
