package service;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YouTubeService youtubeService = new YouTubeService();

    @Override
    public void run() {
        twitchService.execute();
        youtubeService.execute();
    }
}
