package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

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
        final String regex = String.format("\\%s%s\\p{Blank}*", CommonUtil.SIGN, getInstruction());
        final String userZoneId = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        final ZoneId zoneId = StringUtils.isNotBlank(userZoneId) ? ZoneId.of(userZoneId) : ZoneId.systemDefault();

        final String title = String.format("現在時間 (%s)", zoneId);
        final String desc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(zoneId));
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
    }
}
