package Service;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YoutubeService youtubeService = new YoutubeService();

    @Override
    public void run() {
        twitchService.execute();
        youtubeService.execute();
    }
}
