package Service;

import Util.CommonUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YoutubeService youtubeService = new YoutubeService();

    @Override
    public void run() {
        CommonUtil.now = ZonedDateTime.now(ZoneId.of("UTC"));
        twitchService.execute();
        youtubeService.execute();
    }
}
