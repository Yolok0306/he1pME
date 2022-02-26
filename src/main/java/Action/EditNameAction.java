package Action;

import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;

import java.util.Objects;

public class EditNameAction implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent().toString();

        event.getMessage().getMemberMentions().stream().findFirst().ifPresent(partialMember -> {
            final Member member = partialMember.asFullMember().block();
            final String oldName = Objects.requireNonNull(member).getDisplayName();
            final String newName = content.replaceAll("\\$editName\\s<@[!]\\d{18}>\\s++", "");
            final GuildMemberEditSpec guildMemberEditSpec = GuildMemberEditSpec.builder().build();
            Objects.requireNonNull(member).edit(guildMemberEditSpec.withNicknameOrNull(newName)).block();
            final String title = "修改暱稱成功";
            final String desc = oldName + " -> " + newName;
            final String thumb = Objects.requireNonNull(member).getAvatarUrl();
            CommonUtil.replyByHe1pMETemplate(event, title, desc, thumb);
        });
    }
}
