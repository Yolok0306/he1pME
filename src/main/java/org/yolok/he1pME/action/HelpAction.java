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
import org.yolok.he1pME.entity.CallAction;
import org.yolok.he1pME.repository.CallActionRepository;
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
    private CallActionRepository callActionRepository;
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
        addActionEmbed(messageEmbedList, member);
        addMusicEmbed(messageEmbedList, member);
        addCallActionEmbed(messageEmbedList, member);
        message.getChannel().sendMessageEmbeds(messageEmbedList).queue();
    }

    private void addActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<Class<? extends Action>> actionSet = applicationContext.getBeansOfType(Action.class).entrySet()
                .parallelStream()
                .filter(entry -> entry.getValue().getClass().isAnnotationPresent(Help.class))
                .map(Map.Entry::getValue)
                .map(Action::getClass)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(actionSet)) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("一般指令");
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
        embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        messageEmbedList.add(embedBuilder.build());
    }

    private void addMusicEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        Set<Method> musicMethodSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .parallel()
                .filter(method -> method.isAnnotationPresent(Help.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(musicMethodSet)) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("音樂指令");
        musicMethodSet.parallelStream()
                .sorted(Comparator.comparing(Method::getName))
                .map(musicMethod -> musicMethod.getAnnotation(Help.class))
                .forEach(help -> embedBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
        embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        messageEmbedList.add(embedBuilder.build());
    }

    private void addCallActionEmbed(List<MessageEmbed> messageEmbedList, Member member) {
        String guildId = member.getGuild().getId();
        Iterable<CallAction> callActionIterable = callActionRepository.findByGuildId(guildId);
        if (!callActionIterable.iterator().hasNext()) {
            return;
        }

        Map<String, String> callActionMap = new HashMap<>();
        callActionIterable.forEach(callAction -> callActionMap.put(callAction.getAction(), callAction.getDescription()));

        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("客製化指令");
        callActionMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> embedBuilder.addField(CommonUtil.SIGN + entry.getKey(), entry.getValue(), Boolean.FALSE));
        embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        messageEmbedList.add(embedBuilder.build());
    }
}
