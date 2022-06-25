package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.apache.commons.lang3.StringUtils;

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
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}*";
        final String userZoneId = message.getContent().replaceAll(regex, StringUtils.EMPTY);
        final ZoneId zoneId = StringUtils.isNotBlank(userZoneId) ? ZoneId.of(userZoneId) : ZoneId.systemDefault();
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().withZoneSameInstant(zoneId);

        final String title = "現在時間 (" + zoneId + ")";
        final String desc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
    }
}
