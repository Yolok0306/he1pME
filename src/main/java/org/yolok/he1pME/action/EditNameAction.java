package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;
import java.util.Optional;

@Component
@Help(example = "editName @member [name]", description = "為被標記的成員修改名稱")
public class EditNameAction implements Action {

    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<Member> mentionMemberOpt = message.getMentions().getMembers().parallelStream().findFirst();
        Member botMember = member.getGuild().getMember(member.getJDA().getSelfUser());
        if (mentionMemberOpt.isEmpty() || botMember == null) {
            return;
        }

        Member mentionMember = mentionMemberOpt.get();
        String regex = String.format("\\%s%s\\p{Blank}<@\\d{18}>\\p{Blank}++", CommonUtil.SIGN, getInstruction());
        String newName = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        String oldName = mentionMember.getEffectiveName();
        String title, desc;
        if (CommonUtil.notHigher(botMember, mentionMember)) {
            title = "修改暱稱失敗";
            desc = String.format("\"%s\"只能修改最高身分組比自己還低的成員！", botMember.getEffectiveName());
        } else {
            mentionMember.modifyNickname(newName).queue();
            title = "修改暱稱成功";
            desc = String.format("%s -> %s", oldName, newName);
        }
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, mentionMember.getEffectiveAvatarUrl());
    }
}
