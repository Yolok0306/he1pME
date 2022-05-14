package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@help(example = "time [zoneId]", description = "取得現在時間")
public class TimeAction implements Action {
    @Override
    public String getInstruction() {
        return "time";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}*";
        final String userZoneId = event.getMessage().getContent().replaceAll(regex, StringUtils.EMPTY);
        final ZoneId zoneId = StringUtils.isNotBlank(userZoneId) ? ZoneId.of(userZoneId) : ZoneId.systemDefault();
        final ZonedDateTime zonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId);

        final String title = "現在時間 (" + zoneId + ")";
        final String desc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
        CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
    }
}
