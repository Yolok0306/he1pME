package org.yolok.he1pME.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.action.Action;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlashCommandEventService {

    private final CallActionService callActionService;

    private final MusicService musicService;

    @Qualifier("customActionMap")
    private final Map<String, Action> customActionMap;

    private final Set<String> musicActionSet;

    public void execute(SlashCommandInteractionEvent event) {
        try {
            String instruction = event.getName();
            if (musicActionSet.contains(instruction)) {
                executeAction(event, musicService, instruction);
            } else if (customActionMap.containsKey(instruction)) {
                executeAction(event, customActionMap.get(instruction), "execute");
            } else {
                callActionService.execute(event);
            }
        } catch (Exception e) {
            log.error("Failed to execute slash command {}: ", event.getName(), e);
        }
    }

    private void executeAction(SlashCommandInteractionEvent event, Object bean, String methodName) {
        if (CommonUtil.isNotInstructionChannel(event.getMessageChannel().getName())) {
            event.reply("You cannot execute this instruction in this message channel").setEphemeral(true).queue();
            return;
        }

        try {
            MethodUtils.invokeMethod(bean, methodName, event);
        } catch (Exception e) {
            log.error("Failed to invoke method {} on bean {}: ", methodName, bean.getClass().getSimpleName(), e);
        }
    }
}
