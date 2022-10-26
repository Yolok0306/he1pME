package service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YouTubeService youtubeService = new YouTubeService();

    @Override
    public void run() {
        twitchService.execute(ZonedDateTime.now(ZoneId.of("UTC")));
        youtubeService.execute();
    }
}
