package Action;

import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GetCurrentTimeAction implements Action {
    @Override
    public String getInstruction() {
        return "time";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime localDateTime = LocalDateTime.now().atZone(ZoneId.of("Asia/Taipei")).toLocalDateTime();
        final String title = "現在時間 (UTC+8)";
        final String desc = dateTimeFormatter.format(localDateTime);
        CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
    }
}
