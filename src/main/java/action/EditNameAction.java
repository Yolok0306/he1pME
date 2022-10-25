package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

@help(example = "editName @member [name]", description = "為被標記的成員修改名稱")
public class EditNameAction implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMentions().getMembers().stream().findFirst().ifPresent(mentionMember -> {
            final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}<@\\d{18}>\\p{Blank}++";
            final String newName = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
            final String oldName = mentionMember.getNickname();
            mentionMember.modifyNickname(newName).queue();
            final String title = "修改暱稱成功";
            final String desc = oldName + " -> " + newName;
            final String thumb = CommonUtil.getRealAvatarUrl(mentionMember);
            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
        });
    }
}
