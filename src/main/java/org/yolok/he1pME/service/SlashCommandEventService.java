package org.yolok.he1pME.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.action.Action;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.util.CommonUtil;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SlashCommandEventService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CallActionService callActionService;

    private Set<String> musicActionSet;

    private Map<String, Class<? extends Action>> customActionMap;

    @PostConstruct
    public void init() {
        musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .parallel()
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(He1pME.class))
                .map(method -> method.getAnnotation(He1pME.class))
                .map(He1pME::instruction)
                .collect(Collectors.toSet());

        customActionMap = applicationContext.getBeansOfType(Action.class).values()
                .parallelStream()
                .map(Action::getClass)
                .filter(clazz -> clazz.isAnnotationPresent(He1pME.class))
                .collect(Collectors.toMap(clazz -> clazz.getAnnotation(He1pME.class).instruction(), Function.identity()));
    }

    public void execute(SlashCommandInteractionEvent event) {
        String instruction = event.getName();
        if (musicActionSet.contains(instruction)) {
            executeMusicAction(event, instruction);
        } else if (customActionMap.containsKey(instruction)) {
            executeCustomAction(event, customActionMap.get(instruction));
        } else {
            callActionService.execute(event);
        }
    }

    private void executeMusicAction(SlashCommandInteractionEvent event, String instruction) {
        if (CommonUtil.isNotInstructionChannel(event.getMessageChannel().getName())) {
            event.reply("You cannot execute this instruction in this message channel").setEphemeral(true).queue();
            return;
        }

        try {
            Object bean = applicationContext.getBean(MusicService.class);
            MethodUtils.invokeMethod(bean, instruction, event);
        } catch (Exception e) {
            log.error("Failed to execute music action: {}", event.getName(), e);
        }
    }

    private void executeCustomAction(SlashCommandInteractionEvent event, Class<? extends Action> clazz) {
        if (CommonUtil.isNotInstructionChannel(event.getMessageChannel().getName())) {
            event.reply("You cannot execute this instruction in this message channel").setEphemeral(true).queue();
            return;
        }

        try {
            Object bean = applicationContext.getBean(clazz);
            MethodUtils.invokeMethod(bean, "execute", event);
        } catch (Exception e) {
            log.error("Failed to execute custom action: {}", event.getName(), e);
        }
    }
}
