import Service.He1pMEService;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;

import java.util.HashMap;
import java.util.Objects;

public class He1pME {
    private static final He1pMEService he1pMEService = new He1pMEService();

    public static void main(final String[] args) {
        final GatewayDiscordClient bot = DiscordClient.create(getBotToken()).login().block();
        Objects.requireNonNull(bot).getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            System.out.printf("-----Logged in as %s #%s-----%n", self.getUsername(), self.getDiscriminator());
        });
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(he1pMEService::receiveMessage);
        bot.onDisconnect().block();
    }

    private static String getBotToken() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("#key", "id");
        final HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put(":value", "he1pME");
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("Token").query(querySpec);
        final StringBuilder result = new StringBuilder();
        items.forEach(item -> result.append(item.getString("token")));
        return result.toString();
    }
}
