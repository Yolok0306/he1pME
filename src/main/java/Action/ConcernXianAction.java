package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class ConcernXianAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "concernXian";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getUrlFromDB("NeNeYa", IMAGE)).ifPresent(image ->
                replyByXianTemplate(messageChannel, "記得交心得喔~", image));
    }
}
