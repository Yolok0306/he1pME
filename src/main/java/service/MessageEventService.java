package service;

import action.Action;
import annotation.help;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import util.CommonUtil;

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

        actionMap = new Reflections("action").getSubTypesOf(Action.class).stream()
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

    public void execute(final Message message) {
        if (isNotInstructionChannel(message.getChannel())) {
            goodBoyService.checkContent(message);
        } else if (message.getContentRaw().startsWith(CommonUtil.SIGN)) {
            final String instruction = format(message.getContentRaw());
            if (musicActionSet.contains(instruction)) {
                executeMusicAction(message, instruction);
            } else if (actionMap.containsKey(instruction)) {
                executeAction(message, instruction);
            } else {
                callActionService.execute(message, instruction);
            }
        } else if (!message.getContentRaw().startsWith("!") && !Objects.requireNonNull(message.getMember()).getUser().isBot()) {
            message.delete().queue();
        }
    }

    private boolean isNotInstructionChannel(final MessageChannel messageChannel) {
        return !StringUtils.contains(messageChannel.getName(), "指令") &&
                !StringUtils.contains(messageChannel.getName().toLowerCase(), "instruction");
    }

    private String format(final String content) {
        final int indexAfterSIGN = CommonUtil.SIGN.length();
        final int spaceIndex = content.indexOf(StringUtils.SPACE);
        return spaceIndex == -1 ? content.substring(indexAfterSIGN) : content.substring(indexAfterSIGN, spaceIndex);
    }

    private void executeMusicAction(final Message message, final String instruction) {
        try {
            final Method method = MusicService.class.getDeclaredMethod(instruction, Message.class);
            method.invoke(musicService, message);
        } catch (final InvocationTargetException | IllegalAccessException | NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final Message message, final String instruction) {
        try {
            final Class<? extends Action> actionClass = actionMap.get(instruction);
            actionClass.getDeclaredConstructor().newInstance().execute(message);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException |
                       NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }
}
