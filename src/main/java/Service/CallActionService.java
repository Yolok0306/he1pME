package Service;

import Entity.CallAction;
import Util.CommonUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class CallActionService {

    protected void callAction(final MessageChannel messageChannel, final String instruction) {
        getDataFromDB(instruction).ifPresent(item -> {
            final CallAction callAction = buildCallAction(item);
            if (StringUtils.isBlank(callAction.getId()) || Objects.isNull(callAction.getImage())) {
                return;
            }

            messageChannel.createMessage("<@" + callAction.getId() + "> " + callAction.getMessage()).block();
            messageChannel.createMessage(EmbedCreateSpec.create().withColor(callAction.getColor())
                    .withImage(callAction.getImage())).block();
        });
    }

    private Optional<Item> getDataFromDB(final String searchValue) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key = :value")
                .withNameMap(Collections.singletonMap("#key", "action"))
                .withValueMap(Collections.singletonMap(":value", searchValue));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("CallAction").query(querySpec);

        final Item result;
        if (items.iterator().hasNext()) {
            result = items.iterator().next();
        } else {
            result = null;
            log.error("Can not find data with action = \"" + searchValue + "\" in CallAction table !");
        }

        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    private CallAction buildCallAction(final Item item) {
        final CallAction.CallActionBuilder result = CallAction.builder();
        CommonUtil.getMemberDataFromDB(item.getString("name")).ifPresent(memberData -> {
            result.id(memberData.getString("id"));
            final int red = memberData.getNumber("red").intValue();
            final int green = memberData.getNumber("green").intValue();
            final int blue = memberData.getNumber("blue").intValue();
            result.color(Color.of(red, green, blue));
        });
        result.message(item.getString("message"));
        result.image(item.getString("image"));
        return result.build();
    }
}
