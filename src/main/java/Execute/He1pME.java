package Execute;

import Service.He1pMEService;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;

import java.util.Objects;

public class He1pME {
    public static String token;
    private static final He1pMEService he1pMEService = new He1pMEService();

    public static void main(final String[] args) {
        final GatewayDiscordClient bot = DiscordClient.create(token).login().block();
        Objects.requireNonNull(bot).getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            System.out.printf("-----Logged in as %s #%s-----%n", self.getUsername(), self.getDiscriminator());
        });
        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(he1pMEService::receiveMessage);
        bot.onDisconnect().block();
    }
}
