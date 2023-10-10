package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;

@Component
@He1pME(instruction = "edit-nick-name", description = "為被標記的成員修改名稱",
        options = {
                @He1pME.Option(optionType = OptionType.USER, name = "member", description = "member name"),
                @He1pME.Option(name = "nick-name", description = "nick name")
        }, example = "edit-nick=name @member [nick-name]")
public class EditNameAction implements Action {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Member botMember = Objects.requireNonNull(member.getGuild().getMember(member.getJDA().getSelfUser()));
        Member targetMember = Objects.requireNonNull(event.getOption("member")).getAsMember();
        String newName = Objects.requireNonNull(event.getOption("nick-name")).getAsString();
        String oldName = Objects.requireNonNull(targetMember).getNickname();
        String title, desc;
        if (CommonUtil.notHigher(botMember, targetMember)) {
            title = "修改暱稱失敗";
            desc = String.format("\"%s\"只能修改最高身分組比自己還低的成員！", botMember.getEffectiveName());
        } else {
            targetMember.modifyNickname(newName).queue();
            title = "修改暱稱成功";
            desc = String.format("成員 : %s\n名稱變化 : %s -> %s", targetMember.getAsMention(), oldName, newName);
        }
        CommonUtil.replyByHe1pMETemplate(event, member, title, desc, targetMember.getEffectiveAvatarUrl());
    }
}
