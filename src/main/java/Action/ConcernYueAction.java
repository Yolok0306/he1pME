package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class ConcernYueAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "concernYue";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getIdFromDB("Yue")).ifPresent(Yue ->
                Optional.ofNullable(getUrlFromDB("AngryAqua", IMAGE)).ifPresent(img ->
                        replyByDefaultTemplate(messageChannel, Yue, "醒了嗎?", img)));
    }
}
