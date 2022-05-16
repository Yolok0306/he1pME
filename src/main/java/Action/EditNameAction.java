package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
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
    public void execute(final MessageCreateEvent event) {
        event.getMessage().getChannel().subscribe(messageChannel ->
                event.getMember().ifPresent(member ->
                        event.getMessage().getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember -> {
                            final String oldName = partialMember.getDisplayName();
                            final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}<@\\d{18}>\\p{Blank}++";
                            final String newName = event.getMessage().getContent().replaceAll(regex, StringUtils.EMPTY);
                            final GuildMemberEditSpec guildMemberEditSpec = GuildMemberEditSpec.builder().build();
                            partialMember.edit(guildMemberEditSpec.withNicknameOrNull(newName)).block();
                            final String title = "修改暱稱成功";
                            final String desc = oldName + " -> " + newName;
                            final String thumb = partialMember.getAvatarUrl();
                            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                        })
                )
        );
    }
}
