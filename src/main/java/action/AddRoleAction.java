package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import util.CommonUtil;

import java.util.Objects;

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
                    final String title, desc, thumb = mentionMember.getEffectiveAvatarUrl();
                    final Member botMember = member.getGuild().getMember(member.getJDA().getSelfUser());
                    if (CommonUtil.isHigher(member, mentionMember)) {
                        title = "新增身分組失敗";
                        desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", member.getEffectiveName(), role.getName());
                    } else if (CommonUtil.isHigher(Objects.requireNonNull(botMember), role)) {
                        title = "新增身分組失敗";
                        desc = String.format("\"%s\"並未擁有比\"%s\"還高的身分組", botMember.getEffectiveName(), role.getName());
                    } else {
                        message.getGuild().addRoleToMember(mentionMember, role).queue();
                        title = "新增身分組成功";
                        desc = String.format("新增身分組 : %s", role.getName());
                    }
                    CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, thumb);
                })
        );
    }
}
