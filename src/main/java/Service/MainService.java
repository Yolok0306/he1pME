package Service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

class MainService {
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
        try {
            final ItemCollection<QueryOutcome> items = dynamoDB.getTable("PrivateInfo").query(querySpec);
            for (final Item result : items) {
                return Optional.ofNullable(result.getString("token"));
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
        }
        return Optional.empty();
    }

    protected Optional<String> getUrlFromDB(final String tableName, final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        try {
            final ItemCollection<QueryOutcome> items = dynamoDB.getTable(tableName).query(querySpec);
            for (final Item result : items) {
                return Optional.ofNullable(result.getString("url"));
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
        }
        return Optional.empty();
    }

    protected Optional<List<String>> getActionFromDB(final String searchValue) {
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        try {
            final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Action").query(querySpec);
            for (final Item result : items) {
                return Optional.of(Arrays.asList(result.getString("action").split(",")));
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
        }
        return Optional.empty();
    }
}
