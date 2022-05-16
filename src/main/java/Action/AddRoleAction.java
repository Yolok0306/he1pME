package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.util.Objects;

@help(example = "addRole @member @role", description = "為標記的成員新增標記的身分組")
public class AddRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "addRole";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        event.getMessage().getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember ->
                event.getMessage().getRoleMentions().toStream().filter(Objects::nonNull).findFirst().ifPresent(role ->
                        event.getMessage().getChannel().subscribe(messageChannel ->
                                event.getMember().ifPresent(member ->
                                        member.getHighestRole().subscribe(highestRole -> {
                                            final Snowflake roleId = role.getId();
                                            if (CommonUtil.isHigher(highestRole, role) && !partialMember.getRoleIds().contains(roleId)) {
                                                final String reason = "AddRoleAction : " + member.getDisplayName();
                                                partialMember.addRole(roleId, reason).block();
                                                final String title = "新增身分組成功";
                                                final String desc = "新增身分組 : " + role.getName();
                                                final String thumb = partialMember.getAvatarUrl();
                                                CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                                            }
                                        })
                                )
                        )
                )
        );
    }
}
