package Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YoutubeService youtubeService = new YoutubeService();

    @Override
    public void run() {
        twitchService.execute(ZonedDateTime.now(ZoneId.of("UTC")));
        youtubeService.execute();
    }
}
