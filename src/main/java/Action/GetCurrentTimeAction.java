package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class GetCurrentTimeAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "time";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime localDateTime = LocalDateTime.now().atZone(ZoneId.of("Asia/Taipei")).toLocalDateTime();
        replyByHe1pMETemplate(messageChannel, "現在時間", dateTimeFormatter.format(localDateTime));
    }
}
