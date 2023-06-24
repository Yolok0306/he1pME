package org.yolok.he1pME.action;

import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;
import java.util.Optional;

@Component
@Help(example = "addRole @member @role", description = "為被標記的成員新增被標記的身分組")
public class AddRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "addRole";
    }

    @Override
    public void execute(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<Member> mentionMemberOpt = message.getMentions().getMembers().parallelStream().findFirst();
        Optional<Role> mentionRoleOpt = message.getMentions().getRoles().parallelStream().findFirst();
        Member botMember = member.getGuild().getMember(member.getJDA().getSelfUser());
        if (mentionMemberOpt.isEmpty() || mentionRoleOpt.isEmpty() || botMember == null) {
            return;
        }

        Member mentionMember = mentionMemberOpt.get();
        Role mentionRole = mentionRoleOpt.get();
        String title, desc;
        if (mentionMember.getRoles().contains(mentionRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"已經擁有\"%s\"的身分組", mentionMember.getEffectiveName(), mentionRole.getName());
        } else if (CommonUtil.notHigher(member, mentionRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", member.getEffectiveName(), mentionRole.getName());
        } else if (CommonUtil.notHigher(botMember, mentionRole)) {
            title = "新增身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", botMember.getEffectiveName(), mentionRole.getName());
        } else {
            message.getGuild().addRoleToMember(mentionMember, mentionRole).queue();
            title = "新增身分組成功";
            desc = String.format("新增身分組 : %s", mentionRole.getName());
        }
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, mentionMember.getEffectiveAvatarUrl());
    }
}
