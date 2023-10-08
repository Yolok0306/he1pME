package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.service.GoodBoyService;
import org.yolok.he1pME.service.TwitchService;
import org.yolok.he1pME.service.YouTubeService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Component
@Help(example = "reload", description = "重新載入資料庫資料")
public class ReloadAction implements Action {
    @Autowired
    private GoodBoyService goodBoyService;
    @Autowired
    private TwitchService twitchService;
    @Autowired
    private YouTubeService youTubeService;

    @Override
    public String getInstruction() {
        return "reload";
    }

    @Override
    public void execute(Message message) {
        goodBoyService.initBadWordMap();
        twitchService.initNotificationMap();
        youTubeService.initNotificationMap();

        twitchService.adjustCache();
        youTubeService.adjustCache();
        message.delete().queue();

        Member member = Objects.requireNonNull(message.getMember());
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(ZoneId.systemDefault()));
        log.info("Reload Cache by {} at {}", member.getUser().getName(), now);
    }
}
