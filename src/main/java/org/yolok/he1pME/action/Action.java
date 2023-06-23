package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Message;

public interface Action {

    String getInstruction();

    void execute(Message message);
}
