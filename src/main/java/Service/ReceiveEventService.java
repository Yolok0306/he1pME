package Service;

import Action.Action;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ReceiveEventService {
    private final String SIGN = "$";
    private final Set<String> musicActionSet = new HashSet<>();
    private final Map<String, Class<? extends Action>> actionMap = new HashMap<>();
    CallActionService callActionService = new CallActionService();

    public void addMusicActionSet(final Set<String> musicActionSet) {
        this.musicActionSet.addAll(musicActionSet);
    }

    public void putActionMap(final Map<String, Class<? extends Action>> actionMap) {
        this.actionMap.putAll(actionMap);
    }

    public void receiveMessage(final MessageCreateEvent event, final GoodBoyService goodBoyService) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");

        if (Boolean.FALSE.equals(isInstructionChannel(event))) {
            goodBoyService.checkContent(event, content);
        } else if (content.startsWith(SIGN)) {
            final String instruction = format(content);

            if (musicActionSet.contains(instruction)) {
                executeMusicAction(event, instruction);
            } else if (actionMap.containsKey(instruction)) {
                executeAction(event, instruction);
            } else {
                callActionService.callAction(event, instruction);
            }
        } else if (!content.startsWith("!") && event.getMember().isPresent() && !event.getMember().get().isBot()) {
            event.getMessage().delete().block();
        }
    }

    private boolean isInstructionChannel(final MessageCreateEvent event) {
        final AtomicReference<String> channelName = new AtomicReference<>();
        event.getMessage().getChannel().subscribe(messageChannel -> {
            if (messageChannel instanceof TextChannel) {
                final TextChannel textChannel = (TextChannel) messageChannel;
                channelName.set(textChannel.getName());
            }
        });
        return StringUtils.contains(channelName.get(), "指令");
    }

    private String format(final String content) {
        final int spaceIndex = content.indexOf(" ");
        final String instruction = spaceIndex == -1 ? content : content.substring(0, spaceIndex);
        return instruction.replace(SIGN, "");
    }

    private void executeMusicAction(final MessageCreateEvent event, final String response) {
        try {
            final Method method = MusicService.class.getDeclaredMethod(response, MessageCreateEvent.class);
            method.invoke(new MusicService(), event);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final MessageCreateEvent event, final String instruction) {
        final Class<? extends Action> action = actionMap.get(instruction);
        try {
            action.getDeclaredConstructor().newInstance().execute(event);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }
}
