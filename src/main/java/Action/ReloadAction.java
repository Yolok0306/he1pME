package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@help(example = "reload", description = "重新載入禁言資料")
public class ReloadAction implements Action {
    @Override
    public String getInstruction() {
        return "reload";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        CommonUtil.getServerDataFromDB();

        CommonUtil.BAD_WORD_MAP.clear();
        CommonUtil.getBadWordFromDB();

        CommonUtil.TWITCH_NOTIFICATION_MAP.clear();
        CommonUtil.getTwitchNotificationFromDB();

        final ZonedDateTime zonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
        log.info("Reload BAD_WORD_SET by " + member.getTag() + " at " + now);
    }
}
