package service;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();
    private final YouTubeService youtubeService = new YouTubeService();

    @Override
    public void run() {
        final Thread twitchServiceThread = new Thread(twitchService);
        twitchServiceThread.start();
        
        final Thread youTubeServiceThread = new Thread(youtubeService);
        youTubeServiceThread.start();
    }
}
