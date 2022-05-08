package Service;

import Action.Action;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class MessageEventService {
    @Setter
    private Set<String> musicActionSet;
    @Setter
    private Map<String, Class<? extends Action>> actionMap;
    @Getter
    private final GoodBoyService goodBoyService = new GoodBoyService();
    private final CallActionService callActionService = new CallActionService();

    public void receiveEvent(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse(StringUtils.EMPTY);

        if (BooleanUtils.isFalse(isInstructionChannel(event))) {
            goodBoyService.checkContent(event, content);
        } else if (content.startsWith(CommonUtil.SIGN)) {
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
        final int spaceIndex = content.indexOf(StringUtils.SPACE);
        final String instruction = spaceIndex == -1 ? content : content.substring(0, spaceIndex);
        return instruction.substring(CommonUtil.SIGN.length());
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
