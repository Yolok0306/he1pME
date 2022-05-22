package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;

@help(example = "removeRole @member @role", description = "為被標記的成員移除被標記的身分組")
public class RemoveRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "removeRole";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember ->
                message.getRoleMentions().toStream().filter(Objects::nonNull).findFirst().ifPresent(role ->
                        member.getHighestRole().subscribe(highestRole -> {
                            final String title, desc, thumb = partialMember.getAvatarUrl();
                            if (CommonUtil.isNotHigher(highestRole, role)) {
                                title = "移除身分組失敗";
                                desc = member.getDisplayName() + "的權限不足";
                            } else if (partialMember.getRoleIds().contains(role.getId())) {
                                title = "移除身分組失敗";
                                desc = partialMember.getDisplayName() + "並未擁有" + role.getName() + "的身分組";
                            } else {
                                final String reason = "RemoveRoleAction : " + member.getTag();
                                partialMember.removeRole(role.getId(), reason).block();
                                title = "移除身分組成功";
                                desc = "移除身分組 : " + role.getName();
                            }
                            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                        })
                )
        );
    }
}
