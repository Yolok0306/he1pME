package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.service.MusicService;
import org.yolok.he1pME.util.CommonUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Help(example = "help", description = "查看全部指令")
public class HelpAction implements Action {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public String getInstruction() {
        return "help";
    }

    @Override
    public void execute(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        List<MessageEmbed> messageEmbedList = new ArrayList<>();
        addMusicActionEmbed(messageEmbedList, member);
        addCustomActionEmbed(messageEmbedList, member);
        message.getChannel().sendMessageEmbeds(messageEmbedList).queue();
    }

    private void addMusicActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<Method> musicMethodSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .parallel()
                .filter(method -> method.isAnnotationPresent(Help.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(musicMethodSet)) {
            return;
        }

        EmbedBuilder embedBuilder = getEmbedBuilder(member, "音樂指令");
        musicMethodSet.parallelStream()
                .sorted(Comparator.comparing(Method::getName))
                .map(musicMethod -> musicMethod.getAnnotation(Help.class))
                .forEach(help -> embedBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
        messageEmbedList.add(embedBuilder.build());
    }

    private void addCustomActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<Class<? extends Action>> actionSet = applicationContext.getBeansOfType(Action.class).entrySet()
                .parallelStream()
                .filter(entry -> entry.getValue().getClass().isAnnotationPresent(Help.class))
                .map(Map.Entry::getValue)
                .map(Action::getClass)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(actionSet)) {
            return;
        }

        EmbedBuilder embedBuilder = getEmbedBuilder(member, "一般指令");
        actionSet.parallelStream()
                .sorted(Comparator.comparing(action -> {
                    try {
                        return action.getDeclaredConstructor().newInstance().getInstruction();
                    } catch (Exception e) {
                        log.error("Cannot execute `getInstruction()` in {}", action.getName(), e);
                    }
                    return StringUtils.EMPTY;
                }))
                .map(action -> action.getAnnotation(Help.class))
                .forEach(help -> embedBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
        messageEmbedList.add(embedBuilder.build());
    }

    private EmbedBuilder getEmbedBuilder(Member member, String title) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(CommonUtil.HE1PME_COLOR)
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
    }
}
