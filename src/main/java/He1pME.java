import Service.MessageEventService;
import Service.VoiceStateService;
import Util.CommonUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;

import java.util.Optional;

public class He1pME {
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final VoiceStateService voiceStateService = new VoiceStateService();
//    private static final TimerTaskService timerTaskService = new TimerTaskService();
//    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        getTokenDB();
        final IntentSet intentSet = IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.GUILD_VOICE_STATES);
        final GatewayDiscordClient bot = DiscordClient.create(CommonUtil.TOKEN).gateway().setEnabledIntents(intentSet).login().block();

        CommonUtil.BOT = Optional.ofNullable(bot).orElseThrow(() -> new IllegalStateException("Bot token : " + CommonUtil.TOKEN + " is invalid !"));
        CommonUtil.getBadWordFromDB();

        bot.getEventDispatcher().on(ReadyEvent.class).subscribe(event ->
                System.out.printf("-----Logged in as %s #%s-----%n", event.getSelf().getUsername(), event.getSelf().getDiscriminator()));
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(MessageUpdateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(voiceStateService::receiveEvent);
        //        timer.schedule(timerTaskService, 60000, 60000);
        bot.onDisconnect().block();
    }

    private static void getTokenDB() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("#key1 = :value1")
                .withNameMap(new NameMap().with("#key1", "name"))
                .withValueMap(new ValueMap().withString(":value1", "Token"));
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("ServerData").query(querySpec);

        if (items.iterator().hasNext()) {
            CommonUtil.TOKEN = items.iterator().next().getString("id");
        } else {
            throw new IllegalStateException("Can not get Token from ServerData table!");
        }

        dynamoDB.shutdown();
    }
}
