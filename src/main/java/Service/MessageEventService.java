package Service;

import Action.Action;
import Annotation.help;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MessageEventService {
    private final Set<String> musicActionSet;
    private final Map<String, Class<? extends Action>> actionMap;
    private final GoodBoyService goodBoyService = new GoodBoyService();
    private final MusicService musicService = new MusicService();
    private final CallActionService callActionService = new CallActionService();

    public MessageEventService() {
        musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(Objects::nonNull)
                .filter(method -> method.isAnnotationPresent(help.class))
                .filter(method -> Modifier.isProtected(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        actionMap = new Reflections("Action").getSubTypesOf(Action.class).stream()
                .filter(Objects::nonNull)
                .filter(action -> action.isAnnotationPresent(help.class))
                .collect(Collectors.toMap(action -> {
                    try {
                        return action.getDeclaredConstructor().newInstance().getInstruction();
                    } catch (final Exception exception) {
                        exception.printStackTrace();
                    }
                    return StringUtils.EMPTY;
                }, Function.identity(), (existing, replacement) -> existing, HashMap::new));
    }

    public void receiveEvent(final MessageCreateEvent event) {
        final Optional<MessageChannel> messageChannelOpt = event.getMessage().getChannel().blockOptional();
        final Optional<Member> memberOpt = event.getMember();
        if (messageChannelOpt.isEmpty() || memberOpt.isEmpty()) {
            return;
        }

        final Message message = event.getMessage();
        execute(messageChannelOpt.get(), memberOpt.get(), message, message.getContent());
    }

    public void receiveEvent(final MessageUpdateEvent event) {
        final Optional<Message> messageOpt = event.getMessage().blockOptional();
        if (messageOpt.isEmpty()) {
            return;
        }

        final Message message = messageOpt.get();
        final Optional<MessageChannel> messageChannelOpt = event.getChannel().blockOptional();
        final Optional<Member> memberOpt = message.getAuthorAsMember().blockOptional();
        if (messageChannelOpt.isEmpty() || memberOpt.isEmpty()) {
            return;
        }

        execute(messageChannelOpt.get(), memberOpt.get(), message, message.getContent());
    }

    private void execute(final MessageChannel messageChannel, final Member member, final Message message, final String content) {
        if (isNotInstructionChannel(messageChannel)) {
            goodBoyService.checkContent(messageChannel, message, member);
        } else if (content.startsWith(CommonUtil.SIGN)) {
            final String instruction = format(content);

            if (musicActionSet.contains(instruction)) {
                executeMusicAction(messageChannel, message, member, instruction);
            } else if (actionMap.containsKey(instruction)) {
                executeAction(messageChannel, message, member, instruction);
            } else {
                callActionService.execute(messageChannel, message, instruction);
            }
        } else if (!content.startsWith("!") && !member.isBot()) {
            message.delete().block();
        }
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

    private void executeMusicAction(final MessageChannel messageChannel, final Message message, final Member member, final String instruction) {
        try {
            final Method method = MusicService.class.getDeclaredMethod(instruction, MessageChannel.class, Message.class, Member.class);
            method.invoke(musicService, messageChannel, message, member);
        } catch (final InvocationTargetException | IllegalAccessException | NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final MessageChannel messageChannel, final Message message, final Member member, final String instruction) {
        try {
            final Class<? extends Action> actionClass = actionMap.get(instruction);
            actionClass.getDeclaredConstructor().newInstance().execute(messageChannel, message, member);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
                       NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }
}
