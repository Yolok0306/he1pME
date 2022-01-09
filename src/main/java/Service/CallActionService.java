package Service;

import SpecialDataStructure.CallAction;
import SpecialDataStructure.UrlType;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class CallActionService extends CommonService {

    protected void callAction(final MessageCreateEvent event, final String instruction) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        getDataFromDB(instruction).ifPresent(item -> {
            final CallAction callAction = buildCallAction(item);
            if (ObjectUtils.isEmpty(callAction.getId()) || ObjectUtils.isEmpty(callAction.getImage())) {
                return;
            }

            messageChannel.createMessage("<@" + callAction.getId() + "> " + callAction.getMessage()).block();
            messageChannel.createMessage(EmbedCreateSpec.create().withColor(callAction.getColor())
                    .withImage(callAction.getImage())).block();
        });
    }

    private Optional<Item> getDataFromDB(final String searchValue) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final Map<String, String> nameMap = Collections.singletonMap("#key", "action");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("CallAction").query(querySpec);

        if (!items.iterator().hasNext()) {
            log.error("can not find action : " + searchValue + " in database !");
            return Optional.empty();
        }

        final Item result = items.iterator().next();
        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    private CallAction buildCallAction(final Item item) {
        final CallAction.CallActionBuilder result = CallAction.builder();
        getIdFromDB(item.getString("name")).ifPresent(result::id);
        result.message(item.getString("message"));
        getUrlFromDB(item.getString("image"), UrlType.IMAGE).ifPresent(result::image);
        final String[] color = item.getString("color").split(", ");
        result.color(Color.of(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2])));
        return result.build();
    }
}
