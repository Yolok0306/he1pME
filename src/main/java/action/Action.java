package action;

import net.dv8tion.jda.api.entities.Message;

public interface Action {

    String getInstruction();

    void execute(final Message message);
}
