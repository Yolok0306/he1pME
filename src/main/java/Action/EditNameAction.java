package Action;

import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.GuildMemberEditSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class EditNameAction implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        event.getMessage().getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember ->
                partialMember.asFullMember().subscribe(member -> {
                    final String oldName = member.getDisplayName();
                    final String regex = "\\" + CommonUtil.SIGN + "editName\\p{Blank}<@\\d{18}>\\p{Blank}++";
                    final String newName = event.getMessage().getContent().replaceAll(regex, StringUtils.EMPTY);
                    final GuildMemberEditSpec guildMemberEditSpec = GuildMemberEditSpec.builder().build();
                    member.edit(guildMemberEditSpec.withNicknameOrNull(newName)).block();
                    final String title = "修改暱稱成功";
                    final String desc = oldName + " -> " + newName;
                    final String thumb = member.getAvatarUrl();
                    CommonUtil.replyByHe1pMETemplate(event, title, desc, thumb);
                })
        );
    }
}
