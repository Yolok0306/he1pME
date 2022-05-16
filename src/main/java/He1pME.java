import Action.Action;
import Annotation.help;
import Service.MessageEventService;
import Service.MusicService;
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
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class He1pME {
    private static final MessageEventService messageEventService = new MessageEventService();
    private static final VoiceStateService voiceStateService = new VoiceStateService();
//    private static final TimerTaskService timerTaskService = new TimerTaskService();
//    private static final Timer timer = new Timer();

    public static void main(final String[] args) {
        init();
        final IntentSet intentSet = IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.GUILD_VOICE_STATES);
        final GatewayDiscordClient bot = DiscordClient.create(CommonUtil.TOKEN).gateway().setEnabledIntents(intentSet).login().block();

        if (Objects.isNull(bot)) {
            throw new IllegalStateException("Bot token : \"" + CommonUtil.TOKEN + "\" is invalid !");
        }

        CommonUtil.BOT = bot;

        bot.getEventDispatcher().on(ReadyEvent.class).subscribe(event ->
                System.out.printf("-----Logged in as %s #%s-----%n", event.getSelf().getUsername(), event.getSelf().getDiscriminator()));
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(MessageUpdateEvent.class).subscribe(messageEventService::receiveEvent);
        bot.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(voiceStateService::receiveEvent);
        //        timer.schedule(timerTaskService, 60000, 60000);
        bot.onDisconnect().block();
    }

    private static void init() {
        CommonUtil.loadServerDataFromDB();

        messageEventService.setMusicActionSet(Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(Objects::nonNull)
                .filter(method -> method.isAnnotationPresent(help.class))
                .map(Method::getName)
                .collect(Collectors.toSet()));

        messageEventService.setActionMap(new Reflections("Action").getSubTypesOf(Action.class).stream()
                .filter(Objects::nonNull)
                .filter(action -> action.isAnnotationPresent(help.class))
                .collect(Collectors.toMap(action -> {
                    try {
                        return action.getDeclaredConstructor().newInstance().getInstruction();
                    } catch (final Exception exception) {
                        exception.printStackTrace();
                    }
                    return StringUtils.EMPTY;
                }, Function.identity(), (existing, replacement) -> existing, HashMap::new)));
    }
}
