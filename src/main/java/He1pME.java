import Action.Action;
import Service.*;
import Util.CommonUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class He1pME {
    private static String token;
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final VoiceStateService voiceStateService = new VoiceStateService();
    private static final GoodBoyService goodBoyService = new GoodBoyService();
    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        init();
        final IntentSet intentSet = IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.GUILD_VOICE_STATES);
        final GatewayDiscordClient bot = DiscordClient.create(token).gateway().setEnabledIntents(intentSet).login().block();
        Objects.requireNonNull(bot).getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            bot.getGuilds().toStream().forEach(guild -> guild.getMembers().toStream()
                    .filter(member -> member.getRoleIds().contains(CommonUtil.muteRole))
                    .forEach(member -> member.removeRole(CommonUtil.muteRole).block()));
            System.out.printf("-----Logged in as %s #%s-----%n", event.getSelf().getUsername(), event.getSelf().getDiscriminator());
        });
        timer.schedule(new TimerTaskService(bot, goodBoyService), 0, 60000);
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> messageEventService.receiveEvent(event, goodBoyService));
        bot.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(voiceStateService::receiveEvent);
        bot.onDisconnect().block();
    }

    private static void init() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("ServerData").scan(scanSpec);

        if (!items.iterator().hasNext()) {
            throw new IllegalStateException("can not get any server data from database !");
        }

        for (final Item item : items) {
            if (StringUtils.equals(item.getString("name"), "Token")) {
                token = (item.getString("id"));
            } else if (StringUtils.equals(item.getString("name"), "BadWord")) {
                goodBoyService.addBadWordSet(item.getString("id"));
            }
        }
        dynamoDB.shutdown();

        messageEventService.addMusicActionSet(Arrays.stream(MusicService.class.getDeclaredMethods()).filter(method ->
                method.getModifiers() == Modifier.PROTECTED).map(Method::getName).collect(Collectors.toSet()));

        messageEventService.putActionMap(new Reflections("Action").getSubTypesOf(Action.class).stream()
                .collect(Collectors.toMap(action -> {
                    try {
                        return action.getDeclaredConstructor().newInstance().getInstruction();
                    } catch (final Exception exception) {
                        exception.printStackTrace();
                    }
                    return null;
                }, Function.identity(), (existing, replacement) -> existing)));
    }
}
