package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.core.event.domain.message.MessageCreateEvent;
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
    public void execute(final MessageCreateEvent event) {
        event.getMember().ifPresent(member -> {
            CommonUtil.BAD_WORD_SET.clear();
            CommonUtil.loadServerDataFromDB();

            final ZonedDateTime zonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
            final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
            log.info("Reload BAD_WORD_SET by " + member.getUsername() + member.getTag() + " at " + now);
        });
    }
}
