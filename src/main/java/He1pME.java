import Service.MessageEventService;
import Service.TimerTaskService;
import Service.VoiceStateService;
import Util.CommonUtil;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;

import java.util.Optional;
import java.util.Timer;

public class He1pME {
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final VoiceStateService voiceStateService = new VoiceStateService();
    private static final TimerTaskService timerTaskService = new TimerTaskService();
    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        CommonUtil.getServerDataFromDB();
        final IntentSet intentSet = IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.GUILD_VOICE_STATES);
        final GatewayDiscordClient bot = DiscordClient.create(CommonUtil.TOKEN).gateway().setEnabledIntents(intentSet).login().block();

        CommonUtil.BOT = Optional.ofNullable(bot).orElseThrow(() -> new IllegalStateException("Bot token : " + CommonUtil.TOKEN + " is invalid !"));
        CommonUtil.getBadWordFromDB();
        CommonUtil.getTwitchNotificationFromDB();

        bot.getEventDispatcher().on(ReadyEvent.class).subscribe(event ->
                System.out.printf("-----Logged in as %s #%s-----%n", event.getSelf().getUsername(), event.getSelf().getDiscriminator()));
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(MessageUpdateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(voiceStateService::receiveEvent);
        timer.schedule(timerTaskService, 0, CommonUtil.FREQUENCY);
        bot.onDisconnect().block();
    }
}
