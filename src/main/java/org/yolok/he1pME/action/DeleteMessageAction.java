package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@He1pME(instruction = "delete-message", description = "清除多筆文字頻道的訊息",
        options = {
                @He1pME.Option(optionType = OptionType.INTEGER, name = "message-number", description = "message number")
        }, example = "delete-message [message-number]")
public class DeleteMessageAction implements Action {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int number = Objects.requireNonNull(event.getOption("message-number")).getAsInt();
        MessageChannelUnion messageChannelUnion = event.getChannel();
        try {
            List<Message> messageList = messageChannelUnion.getHistoryBefore(event.getId(), number).complete().getRetrievedHistory();
            messageChannelUnion.asTextChannel().deleteMessages(messageList).queue();
        } catch (Exception e) {
            log.error("Cannot execute {}, number = {}", event.getName(), number, e);
        }

        event.reply("Completed!").setEphemeral(true).queue();
    }
}
