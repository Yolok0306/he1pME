package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.entity.CallAction;
import org.yolok.he1pME.service.CallActionService;
import org.yolok.he1pME.service.MusicService;
import org.yolok.he1pME.util.CommonUtil;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@He1pME(instruction = "help", description = "查看全部指令", example = "help")
public class HelpAction implements Action {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CallActionService callActionService;

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        List<MessageEmbed> messageEmbedList = new ArrayList<>();
        addMusicActionEmbed(messageEmbedList, member);
        addCustomActionEmbed(messageEmbedList, member);
        addCallActionEmbed(messageEmbedList, member);
        event.replyEmbeds(messageEmbedList).queue();
    }

    private void addMusicActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<He1pME> musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods()).parallel()
                .filter(method -> method.isAnnotationPresent(He1pME.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getAnnotation(He1pME.class))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(musicActionSet)) {
            return;
        }

        EmbedBuilder embedBuilder = getEmbedBuilder(member, "音樂指令");
        musicActionSet.parallelStream()
                .sorted(Comparator.comparing(He1pME::instruction))
                .forEachOrdered(he1pME -> embedBuilder.addField(CommonUtil.SIGN + he1pME.example(), he1pME.description(), Boolean.FALSE));
        messageEmbedList.add(embedBuilder.build());
    }

    private void addCustomActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<He1pME> customActionSet = applicationContext.getBeansOfType(Action.class).values().parallelStream()
                .map(Action::getClass)
                .filter(clazz -> clazz.isAnnotationPresent(He1pME.class))
                .map(clazz -> clazz.getAnnotation(He1pME.class))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(customActionSet)) {
            return;
        }

        EmbedBuilder embedBuilder = getEmbedBuilder(member, "一般指令");
        customActionSet.parallelStream()
                .sorted(Comparator.comparing(He1pME::instruction))
                .forEachOrdered(he1pME -> embedBuilder.addField(CommonUtil.SIGN + he1pME.example(), he1pME.description(), Boolean.FALSE));
        messageEmbedList.add(embedBuilder.build());
    }

    private void addCallActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        List<CallAction> callActionList = callActionService.getCallActionList(member.getGuild().getId());
        if (CollectionUtils.isEmpty(callActionList)) {
            return;
        }

        EmbedBuilder embedBuilder = getEmbedBuilder(member, "呼叫指令");
        callActionList.parallelStream()
                .sorted(Comparator.comparing(CallAction::getAction))
                .forEachOrdered(callAction -> embedBuilder.addField(CommonUtil.SIGN + callAction.getAction(), callAction.getDescription(), Boolean.FALSE));
        messageEmbedList.add(embedBuilder.build());
    }

    private EmbedBuilder getEmbedBuilder(Member member, String title) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(CommonUtil.HE1PME_COLOR)
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
    }
}
