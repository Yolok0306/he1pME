package Service;

import Action.Action;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class MessageEventService {
    @Setter
    private Set<String> musicActionSet;
    @Setter
    private Map<String, Class<? extends Action>> actionMap;
    @Getter
    private final GoodBoyService goodBoyService = new GoodBoyService();
    private final CallActionService callActionService = new CallActionService();

    public void receiveEvent(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();

        event.getMessage().getChannel().subscribe(messageChannel -> {
            if (isNotInstructionChannel(messageChannel) && event.getMember().isPresent()) {
                goodBoyService.checkContent(event.getMember().get(), event.getMessage(), content, messageChannel);
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
        });
    }

    public void receiveEvent(final MessageUpdateEvent event) {
        if (event.getMessage().blockOptional().isEmpty()) {
            return;
        }

        event.getMessage().subscribe(message -> message.getAuthorAsMember().subscribe(member -> {
            final String content = message.getContent();

            event.getChannel().subscribe(messageChannel -> {
                if (isNotInstructionChannel(messageChannel)) {
                    goodBoyService.checkContent(member, message, content, messageChannel);
                } else if (!content.startsWith(CommonUtil.SIGN) && !content.startsWith("!") && !member.isBot()) {
                    message.delete().block();
                }
            });
        }));
    }

    private boolean isNotInstructionChannel(final MessageChannel messageChannel) {
        if (messageChannel instanceof TextChannel) {
            final TextChannel textChannel = (TextChannel) messageChannel;
            return !StringUtils.contains(textChannel.getName(), "指令");
        }
        return Boolean.FALSE;
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
