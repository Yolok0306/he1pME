package Service;

import Util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimerTask;

@Slf4j
public class TimerTaskService extends TimerTask {
    public static ZonedDateTime now;
    private final TwitchService twitchService = new TwitchService();
    private final YoutubeService youtubeService = new YoutubeService();

    @Override
    public void run() {
        now = ZonedDateTime.now(ZoneId.of("UTC"));
        twitchService.execute();
        youtubeService.execute();
    }

    public static boolean checkStartTime(final String startTimeString) {
        final ZonedDateTime startTime = ZonedDateTime.parse(startTimeString);
        final long interval = Duration.between(startTime, now).toMillis();
        log.info("interval : {}, now : {}, startTime : {}", interval, now, startTime);
        return interval < CommonUtil.FREQUENCY + 50;
    }
}
