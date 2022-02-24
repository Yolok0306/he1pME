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
        if (badWordSet.isEmpty()) {
            return;
        }

        event.getMember().ifPresent(member -> {
            if (member.isBot() || isAdministrator(member) || !isBadWord(content)) {
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

    private boolean isAdministrator(final Member member) {
        return Objects.requireNonNull(member.getBasePermissions().block()).contains(Permission.ADMINISTRATOR);
    }

    private boolean isBadWord(String content) {
        content = content.replaceAll("@everyone|@here", "");
        content = content.replaceAll("<@[!&]\\d{18}>", "");
        content = fullWidthToHalfWidth(content);
        content = content.replaceAll("\\p{Punct}", "");
        content = content.replaceAll("\\p{Blank}", "");

        if (StringUtils.isBlank(content)) {
            return false;
        }

        for (final String badWord : badWordSet) {
            if (badWord.matches("^\\d$") && StringUtils.containsOnly(content, badWord)) {
                return true;
            } else if (!badWord.matches("^\\d$") && StringUtils.contains(content, badWord)) {
                return true;
            }
        }
        return false;
    }

    private String fullWidthToHalfWidth(String content) {
        for (final char c : content.toCharArray()) {
            content = content.replace("　", "");
            if ((int) c >= 65281 && (int) c <= 65374) {
                content = content.replace(c, (char) (((int) c) - 65248));
            }
        }
        return content;
    }

    private String removeMentionInfoAndSymbol(String content) {
        if (content.matches("^.*@everyone.*$")) {
            content = content.replaceAll("@everyone", "");
        }

        if (content.matches("^.*<@[!&]\\d{18}>.*$")) {
            content = content.replaceAll("<@[!&]\\d{18}>", "");
        }

        if (content.matches("^.*\\p{Punct}++.*$")) {
            content = content.replaceAll("\\p{Punct}", "");
        }
        return content;
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
