package org.yolok.he1pME.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.action.Action;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.util.CommonUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageEventService {
    private Set<String> musicActionSet;
    private Map<String, Class<? extends Action>> actionMap;
    @Autowired
    private GoodBoyService goodBoyService;
    @Autowired
    private CallActionService callActionService;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .parallel()
                .filter(method -> method.isAnnotationPresent(Help.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        actionMap = applicationContext.getBeansOfType(Action.class).entrySet()
                .parallelStream()
                .filter(entry -> entry.getValue().getClass().isAnnotationPresent(Help.class))
                .map(Map.Entry::getValue)
                .collect(Collectors.toMap(Action::getInstruction, Action::getClass));
    }

    public void execute(Message message) {
        if (isNotInstructionChannel(message.getChannel())) {
            goodBoyService.checkContent(message);
        } else if (message.getContentRaw().startsWith(CommonUtil.SIGN)) {
            String instruction = format(message.getContentRaw());
            if (musicActionSet.contains(instruction)) {
                executeMusicAction(message, instruction);
            } else if (actionMap.containsKey(instruction)) {
                executeAction(message, actionMap.get(instruction));
            } else {
                callActionService.execute(message, instruction);
            }
        } else if (!message.getContentRaw().startsWith("!") && !Objects.requireNonNull(message.getMember()).getUser().isBot()) {
            message.delete().queue();
        }
    }

    private boolean isNotInstructionChannel(MessageChannel messageChannel) {
        return !StringUtils.contains(messageChannel.getName(), "指令") &&
                !StringUtils.contains(messageChannel.getName().toLowerCase(), "instruction");
    }

    private String format(String content) {
        int indexAfterSIGN = CommonUtil.SIGN.length();
        int spaceIndex = content.indexOf(StringUtils.SPACE);
        return spaceIndex == -1 ? content.substring(indexAfterSIGN) : content.substring(indexAfterSIGN, spaceIndex);
    }

    private void executeMusicAction(Message message, String instruction) {
        try {
            Object bean = applicationContext.getBean(MusicService.class);
            MethodUtils.invokeMethod(bean, instruction, message);
        } catch (Exception e) {
            log.error("Failed to execute music action: {}", message.getContentRaw(), e);
        }
    }

    private void executeAction(Message message, Class<? extends Action> clazz) {
        try {
            Object bean = applicationContext.getBean(clazz);
            MethodUtils.invokeMethod(bean, "execute", message);
        } catch (Exception e) {
            log.error("Failed to execute custom action: {}", message.getContentRaw(), e);
        }
    }
}
