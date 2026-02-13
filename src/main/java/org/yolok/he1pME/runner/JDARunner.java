package org.yolok.he1pME.runner;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.listener.JDAEventListener;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class JDARunner implements CommandLineRunner {

    @Value("${discord.bot.token}")
    private String discordBotToken;

    private final JDAEventListener jdaEventListener;

    private final Set<GatewayIntent> gatewayIntentSet;

    @Override
    public void run(String... args) {
        CommonUtil.JDA = JDABuilder.createDefault(discordBotToken, gatewayIntentSet)
                .addEventListeners(jdaEventListener)
                .disableCache(CacheFlag.SCHEDULED_EVENTS)
                .build();
    }
}
