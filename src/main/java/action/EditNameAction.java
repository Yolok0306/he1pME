package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

import java.util.Objects;

@help(example = "editName @member [name]", description = "為被標記的成員修改名稱")
public class EditNameAction implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMentions().getMembers().stream().findFirst().ifPresent(mentionMember -> {
            final String regex = String.format("\\%s%s\\p{Blank}<@\\d{18}>\\p{Blank}++", CommonUtil.SIGN, getInstruction());
            final String newName = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
            final String oldName = mentionMember.getEffectiveName();
            final String title, desc, thumb = mentionMember.getEffectiveAvatarUrl();
            final Member botMember = member.getGuild().getMember(member.getJDA().getSelfUser());
            if (CommonUtil.isHigher(Objects.requireNonNull(botMember), mentionMember)) {
                title = "修改暱稱失敗";
                desc = String.format("\"%s\"只能修改最高身分組比自己低的成員！", botMember.getEffectiveName());
            } else {
                mentionMember.modifyNickname(newName).queue();
                title = "修改暱稱成功";
                desc = String.format("%s -> %s", oldName, newName);
            }
            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
        });
    }
}
