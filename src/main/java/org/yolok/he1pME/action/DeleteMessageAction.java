package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@He1pME(instruction = "delete-message", description = "清除多筆文字頻道的訊息",
        options = {
                @He1pME.Option(optionType = OptionType.INTEGER, name = "message-number", description = "message number")
        }, example = "delete-message [message-number]")
public class DeleteMessageAction implements Action {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        int number = Objects.requireNonNull(event.getOption("message-number")).getAsInt();
        MessageChannelUnion messageChannelUnion = event.getChannel();
        try {
            List<Message> messageList = messageChannelUnion.getHistoryBefore(event.getId(), number).complete().getRetrievedHistory();
            OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minusWeeks(2);
            Map<Boolean, List<Message>> messageMap = messageList.stream()
                    .collect(Collectors.partitioningBy(msg -> msg.getTimeCreated().isBefore(twoWeeksAgo)));
            List<Message> olderMessages = messageMap.get(true);
            List<Message> recentMessages = messageMap.get(false);
            if (recentMessages.size() == 1) {
                recentMessages.get(0).delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            } else if (recentMessages.size() > 1) {
                messageChannelUnion.asGuildMessageChannel().deleteMessages(recentMessages).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            }
            olderMessages.forEach(msg -> msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
            event.getHook().sendMessage("Completed!").queue();
        } catch (Exception e) {
            log.error("Cannot execute {}, number = {}", event.getName(), number, e);
            event.getHook().sendMessage("Failed to delete messages!").queue();
        }
    }
}
