package org.yolok.he1pME.runner;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.listener.JDAEventListener;
import org.yolok.he1pME.service.TimerTaskService;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Set;
import java.util.Timer;

@Component
public class JDAInitializer implements CommandLineRunner {
    @Value("${discord.bot.token}")
    private String discordBotToken;
    @Autowired
    private JDAEventListener jdaEventListener;
    @Autowired
    private TimerTaskService timerTaskService;
    private Timer timer;
    private Set<GatewayIntent> gatewayIntentSet;

    @PostConstruct
    public void init() {
        timer = new Timer();
        gatewayIntentSet = Set.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
    }

    @Override
    public void run(String... args) {
        CommonUtil.JDA = JDABuilder.createDefault(discordBotToken, gatewayIntentSet).addEventListeners(jdaEventListener).build();
        timer.schedule(timerTaskService, 5000, CommonUtil.FREQUENCY);
    }
}
