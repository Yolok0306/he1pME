package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;

@Component
@He1pME(instruction = "add-role", description = "為被標記的成員新增被標記的身分組",
        options = {
                @He1pME.Option(optionType = OptionType.USER, name = "member", description = "member name"),
                @He1pME.Option(optionType = OptionType.ROLE, name = "role", description = "role name")
        }, example = "add-role @member @role")
public class AddRoleAction implements Action {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Member botMember = Objects.requireNonNull(member.getGuild().getMember(member.getJDA().getSelfUser()));
        Member targetMember = Objects.requireNonNull(event.getOption("member")).getAsMember();
        Role targetRole = Objects.requireNonNull(event.getOption("role")).getAsRole();
        String title, desc;
        if (Objects.requireNonNull(targetMember).getRoles().contains(targetRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"已經擁有\"%s\"的身分組", targetMember.getEffectiveName(), targetRole.getName());
        } else if (CommonUtil.notHigher(member, targetRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", member.getEffectiveName(), targetRole.getName());
        } else if (CommonUtil.notHigher(botMember, targetRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", botMember.getEffectiveName(), targetRole.getName());
        } else {
            Objects.requireNonNull(event.getGuild()).addRoleToMember(targetMember, targetRole).queue();
            title = "新增身分組成功";
            desc = String.format("目標成員 : %s\n新增身分組 : %s", targetMember.getAsMention(), targetRole.getAsMention());
        }
        MessageEmbed he1pMEMessageEmbed = CommonUtil.getHe1pMessageEmbed(member, title, desc, targetMember.getEffectiveAvatarUrl());
        event.replyEmbeds(he1pMEMessageEmbed).queue();
    }
}
