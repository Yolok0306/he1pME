package org.yolok.he1pME.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.entity.CallAction;
import org.yolok.he1pME.entity.MemberData;
import org.yolok.he1pME.repository.CallActionRepository;
import org.yolok.he1pME.repository.MemberDataRepository;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class CallActionService {

    @Autowired
    private CallActionRepository callActionRepository;

    @Autowired
    private MemberDataRepository memberDataRepository;

    public void execute(SlashCommandInteractionEvent event) {
        String instruction = event.getName();
        String guildId = Objects.requireNonNull(event.getGuild()).getId();
        Optional<CallAction> callActionOpt = callActionRepository.findByActionAndGuildId(instruction, guildId);
        if (callActionOpt.isEmpty()) {
            log.error("Unable to get data for name = {} and guild_id = {} in CallAction table!", instruction, guildId);
            return;
        }

        CallAction callAction = callActionOpt.get();
        Optional<MemberData> memberDataOpt = memberDataRepository.findByNameAndGuildId(callAction.getName(), guildId);
        if (memberDataOpt.isEmpty()) {
            log.error("Unable to get data for name = {} and guild_id = {} in CallAction table!", callAction.getName(), guildId);
            return;
        }

        MemberData memberData = memberDataOpt.get();
        Color color = new Color(memberData.getRed(), memberData.getGreen(), memberData.getBlue());
        String content = String.format("<@%s> %s", memberData.getMemberId(), callAction.getMessage());
        MessageEmbed messageEmbed = new EmbedBuilder().setColor(color).setImage(callAction.getImage()).build();
        event.reply(content).addEmbeds(messageEmbed).queue();
    }

    public List<SlashCommandData> getSlashCommandDataList(String guildId) {
        return callActionRepository.findByGuildId(guildId).parallelStream()
                .map(callAction -> Commands.slash(callAction.getAction(), callAction.getDescription()))
                .toList();
    }
}
