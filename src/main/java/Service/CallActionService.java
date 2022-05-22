package Service;

import Entity.CallAction;
import Util.CommonUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Slf4j
public class CallActionService {

    protected void callAction(final MessageChannel messageChannel, final Message message, final String instruction) {
        if (message.getGuildId().isEmpty()) {
            log.error(CommonUtil.SIGN + instruction + "can not execute because GuildId is null!");
            return;
        }

        final String guildId = message.getGuildId().get().asString();
        getCallActionFromDB(instruction, guildId).flatMap(item -> buildCallAction(item, guildId)).ifPresent(callAction -> {
            final String content = "<@" + callAction.getId() + ">" + StringUtils.SPACE + callAction.getMessage();
            final EmbedCreateSpec embed = EmbedCreateSpec.create().withColor(callAction.getColor()).withImage(callAction.getImage());
            messageChannel.createMessage(content).withEmbeds(embed).block();
        });
    }

    private Optional<Item> getCallActionFromDB(final String action, final String guildId) {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key1 = :value1 and #key2 = :value2")
                .withNameMap(new NameMap().with("#key1", "action").with("#key2", "guild_id"))
                .withValueMap(new ValueMap().withString(":value1", action).withString(":value2", guildId));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("CallAction").query(querySpec);

        final Item result;
        if (items.iterator().hasNext()) {
            result = items.iterator().next();
        } else {
            result = null;
            log.error("Can not find data with name = \"" + action + "\" and guild_id = \"" + guildId + "\" in CallAction Table!");
        }

        dynamoDB.shutdown();
        return Optional.ofNullable(result);
    }

    private Optional<CallAction> buildCallAction(final Item item, final String guildId) {
        final Optional<Item> itemOpt = CommonUtil.getMemberDataFromDB(item.getString("name"), guildId);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }

        final Item memberData = itemOpt.get();
        final int red = memberData.getNumber("red").intValue();
        final int green = memberData.getNumber("green").intValue();
        final int blue = memberData.getNumber("blue").intValue();
        final CallAction result = CallAction.builder()
                .id(memberData.getString("member_id"))
                .color(Color.of(red, green, blue))
                .message(item.getString("message"))
                .image(item.getString("image"))
                .build();
        return Optional.ofNullable(result);
    }
}
