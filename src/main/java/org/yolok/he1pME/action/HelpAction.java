package org.yolok.he1pME.action;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.service.CallActionService;
import org.yolok.he1pME.util.CommonUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@He1pME(instruction = "help", description = "顯示幫助頁面", example = "help")
@RequiredArgsConstructor
public class HelpAction implements Action {

    private final CallActionService callActionService;

    private final List<Action> actions;

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String title = "幫助頁面";
        String desc = actions.stream()
                .map(action -> action.getClass().getAnnotation(He1pME.class))
                .filter(Objects::nonNull)
                .map(he1pME -> CommonUtil.descFormat(he1pME.instruction() + " : " + he1pME.description()))
                .collect(Collectors.joining(StringUtils.LF));

        String guildId = Objects.requireNonNull(event.getGuild()).getId();
        String callActionDesc = callActionService.getCallActionList(guildId).stream()
                .map(callAction -> CommonUtil.descFormat(callAction.getAction() + " : " + callAction.getDescription()))
                .collect(Collectors.joining(StringUtils.LF));

        if (StringUtils.isNotBlank(callActionDesc)) {
            desc += StringUtils.LF + callActionDesc;
        }

        MessageEmbed he1pMEMessageEmbed = CommonUtil.getHe1pMessageEmbed(Objects.requireNonNull(event.getMember()), title, desc, null);
        event.replyEmbeds(he1pMEMessageEmbed).queue();
    }
}
