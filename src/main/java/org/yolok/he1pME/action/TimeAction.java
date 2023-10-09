package org.yolok.he1pME.action;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.util.CommonUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
@Help(example = "time [zoneId]", description = "取得現在時間")
public class TimeAction implements Action {

    @Override
    public String getInstruction() {
        return "time";
    }

    @Override
    public void execute(Message message) {
        String regex = String.format("\\%s%s\\p{Blank}*", CommonUtil.SIGN, getInstruction());
        String userZoneId = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        ZoneId zoneId = StringUtils.isNotBlank(userZoneId) ? ZoneId.of(userZoneId) : ZoneId.systemDefault();

        Member member = Objects.requireNonNull(message.getMember());
        String title = String.format("現在時間 (%s)", zoneId);
        String desc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(zoneId));
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, null);
    }
}
