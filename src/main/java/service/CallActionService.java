package service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import entity.CallAction;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import util.CommonUtil;

import java.awt.*;
import java.util.Optional;

@Slf4j
public class CallActionService {

    protected void execute(final MessageChannel messageChannel, final Message message, final String instruction) {
        final CallAction callAction = getCallActionFromDB(instruction, message.getGuild().getId());
        if (callAction == null) {
            return;
        }
        
        final String content = String.format("<@%s> %s", callAction.getId(), callAction.getMessage());
        final MessageEmbed messageEmbed = new EmbedBuilder().setColor(callAction.getColor())
                .setImage(callAction.getImage()).build();
        messageChannel.sendMessage(content).addEmbeds(messageEmbed).queue();
    }

    private CallAction getCallActionFromDB(final String action, final String guildId) {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(CommonUtil.REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(CommonUtil.BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key1 = :value1 and #key2 = :value2")
                .withNameMap(new NameMap().with("#key1", "action").with("#key2", "guild_id"))
                .withValueMap(new ValueMap().withString(":value1", action).withString(":value2", guildId));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("CallAction").query(querySpec);

        final CallAction result;
        if (items.iterator().hasNext()) {
            final Item item = items.iterator().next();
            result = buildCallAction(item, guildId);
        } else {
            log.error("Unable to get data for name = {} and guild_id = {} in CallAction table!", action, guildId);
            return null;
        }

        dynamoDB.shutdown();
        return result;
    }

    private CallAction buildCallAction(final Item item, final String guildId) {
        final Optional<Item> memberDataOpt = CommonUtil.getMemberDataFromDB(item.getString("name"), guildId);
        if (memberDataOpt.isEmpty()) {
            return null;
        }

        final Item memberData = memberDataOpt.get();
        final int red = memberData.getNumber("red").intValue();
        final int green = memberData.getNumber("green").intValue();
        final int blue = memberData.getNumber("blue").intValue();
        return CallAction.builder().id(memberData.getString("member_id")).color(new Color(red, green, blue))
                .message(item.getString("message")).image(item.getString("image")).build();
    }
}
