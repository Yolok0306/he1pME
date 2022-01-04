package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;

import java.util.Objects;

public class EditNameAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String context = event.getMessage().getContent().toString();

        event.getMessage().getMemberMentions().stream().findFirst().ifPresent(partialMember -> {
            final Member member = partialMember.asFullMember().block();
            final String oldName = Objects.requireNonNull(member).getDisplayName();
            final String newName = context.split(" ").length < 3 ?
                    Objects.requireNonNull(member).getUsername() : getNewName(context);
            final GuildMemberEditSpec guildMemberEditSpec = GuildMemberEditSpec.builder().build();
            Objects.requireNonNull(member).edit(guildMemberEditSpec.withNicknameOrNull(newName)).block();
            final String title = "修改暱稱成功";
            final String desc = oldName + " -> " + newName;
            final String thumb = Objects.requireNonNull(member).getAvatarUrl();
            replyByHe1pMETemplate(event, title, desc, thumb);
        });
    }

    private String getNewName(final String context) {
        final int index = context.indexOf(" ", 10);
        String result = context.substring(index + 1);
        while (result.startsWith(" ")) {
            result = result.substring(1);
        }
        return result;
    }
}
