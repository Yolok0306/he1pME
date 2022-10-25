package action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public interface Action {

    String getInstruction();

    void execute(final MessageChannel messageChannel, final Message message, final Member member);
}
