package org.yolok.he1pME.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

@Slf4j
@Configuration
public class CommonUtil {

    public static JDA JDA;

    public static long FREQUENCY;

    public static Color HE1PME_COLOR = new Color(255, 192, 203);

    public static MessageEmbed getHe1pMessageEmbed(Member member, String title, String desc, String thumb) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc)
                .setColor(HE1PME_COLOR)
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        if (StringUtils.isNotBlank(thumb)) {
            embedBuilder.setThumbnail(thumb);
        }
        return embedBuilder.build();
    }

    public static boolean checkStartTime(String startTimeString) {
        ZonedDateTime startTime = ZonedDateTime.parse(startTimeString);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        return Duration.between(startTime, now).toSeconds() >= Duration.ofMillis(FREQUENCY).toSeconds();
    }

    public static String descFormat(String desc) {
        return StringUtils.abbreviate(desc, 43);
    }

    public static String descStartWithDiamondFormat(String desc) {
        return StringUtils.abbreviate(desc, 36);
    }

    public static boolean notHigher(Member source, Member target) {
        Integer sourcePosition = source.getRoles().parallelStream()
                .map(Role::getPosition)
                .max(Comparator.comparing(Integer::intValue))
                .orElse(0);
        Integer targetPosition = target.getRoles().parallelStream()
                .map(Role::getPosition)
                .max(Comparator.comparing(Integer::intValue))
                .orElse(0);
        return sourcePosition <= targetPosition || target.getPermissions().contains(Permission.ADMINISTRATOR);
    }

    public static boolean notHigher(Member member, Role role) {
        return member.getRoles().parallelStream().noneMatch(eachRole -> eachRole.getPosition() > role.getPosition());
    }

    public static boolean isNotInstructionChannel(String messageChannelName) {
        return !StringUtils.contains(messageChannelName, "指令") &&
                !StringUtils.containsIgnoreCase(messageChannelName, "instruction");
    }

    @Value("${frequency}")
    private void setFrequency(long frequency) {
        FREQUENCY = frequency;
    }
}
