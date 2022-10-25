package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import util.CommonUtil;

@help(example = "addRole @member @role", description = "為被標記的成員新增被標記的身分組")
public class AddRoleAction implements Action {
    @Override
    public String getInstruction() {
        return "addRole";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        message.getMentions().getMembers().stream().findFirst().ifPresent(mentionMember ->
                message.getMentions().getRoles().stream().findFirst().ifPresent(role -> {
                    final String title, desc, thumb = CommonUtil.getRealAvatarUrl(mentionMember);
                    if (member.getPermissions().size() <= mentionMember.getPermissions().size()) {
                        title = "新增身分組失敗";
                        desc = member.getNickname() + "的權限不足";
                    } else if (mentionMember.getRoles().contains(role)) {
                        title = "新增身分組失敗";
                        desc = mentionMember.getNickname() + "已經擁有" + role.getName() + "的身分組了";
                    } else {
                        final String reason = "AddRoleAction : " + member.getUser().getAsTag();
                        message.getGuild().addRoleToMember(mentionMember, role).queue();
                        title = "新增身分組成功";
                        desc = "新增身分組 : " + role.getName();
                    }
                    CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                })
        );
    }
}
