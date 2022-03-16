package Action;

import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class AddRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "addRole";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        event.getMessage().getMemberMentions().stream().findFirst().ifPresent(partialMember ->
                event.getMessage().getRoleMentions().toStream().findFirst().ifPresent(role ->
                        partialMember.asFullMember().subscribe(member -> {
                            final String sponsor = event.getMember().isPresent() ? event.getMember().get().getDisplayName() : "";
                            member.addRole(role.getId(), "AddRoleAction : " + sponsor).block();
                            final String title = "新增身分組成功";
                            final String desc = "新增身分組 : " + role.getName();
                            final String thumb = member.getAvatarUrl();
                            CommonUtil.replyByHe1pMETemplate(event, title, desc, thumb);
                        })
                )
        );
    }
}
