package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;
import java.util.Optional;

@Component
@Help(example = "removeRole @member @role", description = "為被標記的成員移除被標記的身分組")
public class RemoveRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "removeRole";
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
        if (!mentionMember.getRoles().contains(mentionRole)) {
            title = "移除身分組失敗";
            desc = String.format("\"%s\"並未擁有\"%s\"的身分組", mentionMember.getEffectiveName(), mentionRole.getName());
        } else if (CommonUtil.notHigher(member, mentionRole)) {
            title = "移除身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", member.getEffectiveName(), mentionRole.getName());
        } else if (CommonUtil.notHigher(botMember, mentionRole)) {
            title = "移除身分組失敗";
            desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", botMember.getEffectiveName(), mentionRole.getName());
        } else {
            message.getGuild().removeRoleFromMember(mentionMember, mentionRole).queue();
            title = "移除身分組成功";
            desc = String.format("移除身分組 : %s", mentionRole.getName());
        }
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, mentionMember.getEffectiveAvatarUrl());
    }
}
