package Service;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GoodBoyService extends CommonService {
    private final Set<String> badWordSet = new HashSet<>();
    private final Map<Member, Integer> badBoyMap = new HashMap<>();
    private final Snowflake muteRole = Snowflake.of("836214787918528582");

    public void addBadWordSet(final String badWord) {
        badWordSet.add(badWord);
    }

    protected void checkContent(final MessageCreateEvent event, final String content) {
        if (badWordSet.isEmpty() || !isBadWord(content)) {
            return;
        }

        event.getMember().ifPresent(member -> {
            if (member.isBot() || Objects.requireNonNull(member.getBasePermissions().block()).contains(Permission.ADMINISTRATOR)) {
                return;
            }

            event.getMessage().delete().block();
            member.addRole(muteRole, "Bad Boy").block();
            final String title = "言論審查系統";
            final String desc = "◆ 不當言論 : " + content + "\n◆ 懲處 : 禁言3分鐘";
            replyByHe1pMETemplate(event, title, desc, null);
            badBoyMap.put(member, 3);
        });
    }

    private boolean isBadWord(final String content) {
        for (final String badWord : badWordSet) {
            if (StringUtils.containsIgnoreCase(content, badWord)) {
                return true;
            }
        }
        return false;
    }

    protected void updateMap() {
        if (badBoyMap.isEmpty()) {
            return;
        }

        for (final Map.Entry<Member, Integer> entry : badBoyMap.entrySet()) {
            if (entry.getValue() > 1) {
                badBoyMap.replace(entry.getKey(), entry.getValue() - 1);
            } else {
                entry.getKey().removeRole(muteRole, "Good Boy").block();
                badBoyMap.remove(entry.getKey());
            }
        }
    }
}
