package Action;

import discord4j.core.event.domain.message.MessageCreateEvent;

public interface Action {

    String getInstruction();

    void execute(final MessageCreateEvent event);
}
