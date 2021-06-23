import Service.He1pMEService;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class He1pME {
    private static final He1pMEService he1pMEService = new He1pMEService();

    public static void main(final String[] args) {
        final GatewayDiscordClient bot = DiscordClient.create(getTOKEN()).login().block();

        Objects.requireNonNull(bot).getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            System.out.printf("-----Logged in as %s #%s-----%n", self.getUsername(), self.getDiscriminator());
        });

        bot.getEventDispatcher().on(MessageCreateEvent.class).subscribe(he1pMEService::chat);

        bot.onDisconnect().block();
    }

    private static String getTOKEN() {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/privateInfo.json"));
            return jsonObject.get("TOKEN").toString();
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
}
