package org.yolok.he1pME.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.entity.CallAction;
import org.yolok.he1pME.entity.MemberData;
import org.yolok.he1pME.repository.CallActionRepository;
import org.yolok.he1pME.repository.MemberDataRepository;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallActionService {

    private final CallActionRepository callActionRepository;

    private final MemberDataRepository memberDataRepository;

    public void execute(SlashCommandInteractionEvent event) {
        String instruction = event.getName();
        String guildId = Objects.requireNonNull(event.getGuild()).getId();
        Optional<CallAction> callActionOpt = callActionRepository.findByActionAndGuildId(instruction, guildId);
        if (callActionOpt.isEmpty()) {
            log.error("Unable to get data for name = {} and guild_id = {} in CallAction table!", instruction, guildId);
            return;
        }

        CallAction callAction = callActionOpt.get();
        List<MemberData> memberDataList = Arrays.stream(callAction.getMemberNames().split(", "))
                .parallel()
                .map(name -> memberDataRepository.findByNameAndGuildId(name, guildId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Color color = null;
        if (CollectionUtils.size(memberDataList) == 1) {
            MemberData memberData = memberDataList.get(0);
            color = new Color(memberData.getRed(), memberData.getGreen(), memberData.getBlue());
        }

        String content = memberDataList.stream()
                .map(memberData -> "<@" + memberData.getMemberId() + ">")
                .collect(Collectors.joining(StringUtils.SPACE, StringUtils.EMPTY, StringUtils.SPACE + callAction.getMessage()));

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(color)
                .setImage(callAction.getImage())
                .build();

        event.reply(content).addEmbeds(messageEmbed).queue();
    }

    public List<CallAction> getCallActionList(String guildId) {
        return callActionRepository.findByGuildId(guildId);
    }
}
