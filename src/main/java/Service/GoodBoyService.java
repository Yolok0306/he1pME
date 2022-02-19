package Service;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GoodBoyService extends CommonService {
    private final Map<Member, Integer> badBoyMap = new HashMap<>();
    protected final Set<String> badWordSet = new HashSet<>();

    protected void checkContent(final MessageCreateEvent event, final String content) {
        if (badWordSet.contains(content)) {
            event.getMember().ifPresent(member -> {
                if (!hasAccessRight(member)) {
                    event.getMessage().delete().block();
                    member.addRole(Snowflake.of("836214787918528582"), "Bad Boy").block();
                    final String title = "言論審查系統通知";
                    final String desc = "不當言論 : " + content + "\n懲處 : 禁言3分鐘";
                    replyByHe1pMETemplate(event, title, desc, null);
                    badBoyMap.put(member, 0);
                }
            });
        }
    }

    private Boolean hasAccessRight(final Member member) {
        return Boolean.TRUE.equals(member.isHigher(Snowflake.of("855766229963898891")).block()) ||
                member.getRoleIds().contains(Snowflake.of("880413244416753674"));
    }

    protected void updateMap() {
        if (badBoyMap.isEmpty()) {
            return;
        }

        for (final Map.Entry<Member, Integer> entry : badBoyMap.entrySet()) {
            if (entry.getValue() < 2) {
                badBoyMap.replace(entry.getKey(), entry.getValue() + 1);
            } else {
                entry.getKey().removeRole(Snowflake.of("836214787918528582"), "Good Boy").block();
                badBoyMap.remove(entry.getKey());
            }
        }
    }
}
