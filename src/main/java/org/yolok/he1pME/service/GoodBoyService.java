package org.yolok.he1pME.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.util.CommonUtil;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GoodBoyService {

    private final Map<String, Set<String>> badWordMap;

    private static final Pattern EVERYONE_HERE_PATTERN = Pattern.compile("@everyone|@here");
    private static final Pattern MEMBER_MENTION_PATTERN = Pattern.compile("<@[!&]?\\d{18}>");
    private static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("<#\\d{18}>");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");
    private static final Pattern BLANK_PATTERN = Pattern.compile("\\p{Blank}");

    public GoodBoyService(@Qualifier("badWordMap") Map<String, Set<String>> badWordMap) {
        this.badWordMap = badWordMap;
    }

    public void checkContent(Message message) {
        try {
            String guildId = message.getGuild().getId();
            if (!badWordMap.containsKey(guildId)) {
                return;
            }

            Set<String> badWordSet = badWordMap.get(guildId);
            Member member = Objects.requireNonNull(message.getMember());
            
            if (member.getUser().isBot() || hasModerationPermission(member)) {
                return;
            }

            String content = message.getContentRaw();
            if (!isBadWord(content, badWordSet)) {
                return;
            }

            punishMember(message, member, content);
        } catch (Exception e) {
            log.error("GoodBoyService checkContent failed: ", e);
        }
    }

    private boolean hasModerationPermission(Member member) {
        EnumSet<Permission> permissions = member.getPermissions();
        return permissions.contains(Permission.ADMINISTRATOR) || permissions.contains(Permission.MODERATE_MEMBERS);
    }

    private void punishMember(Message message, Member member, String content) {
        message.delete().queue(null, e -> {
            log.error("Failed to delete offensive message: ", e);
        });

        int punishmentTime = 3;
        member.timeoutFor(punishmentTime, TimeUnit.MINUTES).queue(
                null, 
                e -> {
                    log.error("Failed to timeout member {}: ", member.getEffectiveName(), e);
                }
        );

        String title = "言論審查系統";
        String desc = String.format("◆ 不當言論 : %s\n◆ 懲處 : 禁言%d分鐘", content, punishmentTime);
        MessageEmbed embed = CommonUtil.getHe1pMessageEmbed(member, title, desc, null);
        message.getChannel().sendMessageEmbeds(embed).queue();
    }

    private boolean isBadWord(String content, Set<String> badWordSet) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        String normalizedContent = normalizeContent(content);
        if (StringUtils.isBlank(normalizedContent)) {
            return false;
        }

        String lowerContent = normalizedContent.toLowerCase();
        for (String badWord : badWordSet) {
            String lowerBadWord = badWord.toLowerCase();
            if (badWord.length() == 1) {
                if (StringUtils.containsOnly(lowerContent, lowerBadWord)) {
                    return true;
                }
            } else {
                if (StringUtils.contains(lowerContent, lowerBadWord)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeContent(String content) {
        content = EVERYONE_HERE_PATTERN.matcher(content).replaceAll(StringUtils.EMPTY);
        content = MEMBER_MENTION_PATTERN.matcher(content).replaceAll(StringUtils.EMPTY);
        content = CHANNEL_MENTION_PATTERN.matcher(content).replaceAll(StringUtils.EMPTY);
        content = fullWidthToHalfWidth(content);
        content = PUNCTUATION_PATTERN.matcher(content).replaceAll(StringUtils.EMPTY);
        content = BLANK_PATTERN.matcher(content).replaceAll(StringUtils.EMPTY);
        return content;
    }

    private String fullWidthToHalfWidth(String content) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        
        StringBuilder sb = new StringBuilder(content.length());
        for (char c : content.toCharArray()) {
            if (c == '　') {
                continue;
            }
            if (c >= 65281 && c <= 65374) {
                sb.append((char) (c - 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
