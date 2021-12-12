package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;

import java.util.Objects;

public class ChangeNameAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "editName";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String[] array = event.getMessage().getContent().toString().split(" ");
        final String name = array[array.length - 1];
        event.getMessage().getMemberMentions().forEach(partialMember -> {
            final Member member = partialMember.asFullMember().block();
            final GuildMemberEditSpec guildMemberEditSpec = GuildMemberEditSpec.builder().build();
            Objects.requireNonNull(member).edit(guildMemberEditSpec.withNicknameOrNull(name)).block();
        });
    }
}
