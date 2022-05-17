package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;

@help(example = "addRole @member @role", description = "為標記的成員新增標記的身分組")
public class AddRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "addRole";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMemberMentions().stream().filter(Objects::nonNull).findFirst().ifPresent(partialMember ->
                message.getRoleMentions().toStream().filter(Objects::nonNull).findFirst().ifPresent(role ->
                        member.getHighestRole().subscribe(highestRole -> {
                            final String title, desc, thumb;
                            if (CommonUtil.isNotHigher(highestRole, role)) {
                                title = "新增身分組失敗";
                                desc = member.getDisplayName() + "的權限不足";
                                thumb = null;
                            } else if (partialMember.getRoleIds().contains(role.getId())) {
                                title = "新增身分組失敗";
                                desc = partialMember.getDisplayName() + "已經擁有" + role.getName() + "的身分組";
                                thumb = null;
                            } else {
                                final String reason = "AddRoleAction : " + member.getTag();
                                partialMember.addRole(role.getId(), reason).block();
                                title = "新增身分組成功";
                                desc = "新增身分組 : " + role.getName();
                                thumb = partialMember.getAvatarUrl();
                            }
                            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                        })
                )
        );
    }
}
