package org.yolok.he1pME.runner;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.listener.JDAEventListener;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Set;

@Component
public class JDARunner implements CommandLineRunner {

    @Value("${discord.bot.token}")
    private String discordBotToken;

    @Autowired
    private JDAEventListener jdaEventListener;

    private Set<GatewayIntent> gatewayIntentSet;

    @PostConstruct
    public void init() {
        gatewayIntentSet = Set.of(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        );
    }

    @Override
    public void run(String... args) {
        CommonUtil.JDA = JDABuilder.createDefault(discordBotToken, gatewayIntentSet)
                .addEventListeners(jdaEventListener)
                .disableCache(CacheFlag.SCHEDULED_EVENTS)
                .build();
    }
}
