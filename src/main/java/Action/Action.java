package Action;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

public interface Action {

    String getInstruction();

    void execute(final MessageChannel messageChannel, final Message message, final Member member);
}
