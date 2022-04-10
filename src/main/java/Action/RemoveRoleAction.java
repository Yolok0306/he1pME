package Action;

import Util.CommonUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class RemoveRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "removeRole";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        event.getMessage().getMemberMentions().stream().findFirst().ifPresent(partialMember ->
                event.getMessage().getRoleMentions().toStream().findFirst().ifPresent(role ->
                        event.getMember().ifPresent(member ->
                                member.getHighestRole().subscribe(highestRole -> {
                                    final Snowflake roleId = role.getId();
                                    if (CommonUtil.isHigher(highestRole, role) && partialMember.getRoleIds().contains(roleId)) {
                                        final String reason = "RemoveRoleAction : " + member.getDisplayName();
                                        partialMember.removeRole(roleId, reason).block();
                                        final String title = "移除身分組成功";
                                        final String desc = "移除身分組 : " + role.getName();
                                        final String thumb = partialMember.getAvatarUrl();
                                        CommonUtil.replyByHe1pMETemplate(event, title, desc, thumb);
                                    }
                                })
                        )
                )
        );
    }
}
