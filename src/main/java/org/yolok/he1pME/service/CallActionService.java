package org.yolok.he1pME.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.entity.CallAction;
import org.yolok.he1pME.entity.MemberData;
import org.yolok.he1pME.repository.CallActionRepository;
import org.yolok.he1pME.repository.MemberDataRepository;

import java.awt.*;
import java.util.Optional;

@Slf4j
@Service
public class CallActionService {
    @Autowired
    private CallActionRepository callActionRepository;

    @Autowired
    private MemberDataRepository memberDataRepository;

    protected void execute(Message message, String instruction) {
        String guildId = message.getGuild().getId();
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
        message.getChannel().sendMessage(content).addEmbeds(messageEmbed).queue();
    }
}
