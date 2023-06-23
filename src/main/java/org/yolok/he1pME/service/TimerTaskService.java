package org.yolok.he1pME.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.TimerTask;

@Service
public class TimerTaskService extends TimerTask {
    @Autowired
    private TwitchService twitchService;
    @Autowired
    private YouTubeService youtubeService;

    @Override
    public void run() {
        Thread twitchServiceThread = new Thread(twitchService);
        twitchServiceThread.start();

        Thread youTubeServiceThread = new Thread(youtubeService);
        youTubeServiceThread.start();
    }
}
