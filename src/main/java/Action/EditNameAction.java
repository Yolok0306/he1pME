package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.GuildMemberEditSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@help(example = "editName @member [new name]", description = "修改標記成員的名稱")
public class EditNameAction implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember -> {
            final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}<@\\d{18}>\\p{Blank}++";
            final String newName = message.getContent().replaceAll(regex, StringUtils.EMPTY);
            final String oldName = partialMember.getDisplayName();
            partialMember.edit(GuildMemberEditSpec.builder().build().withNicknameOrNull(newName)).block();
            final String title = "修改暱稱成功";
            final String desc = oldName + " -> " + newName;
            final String thumb = partialMember.getAvatarUrl();
            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
        });
    }
}
