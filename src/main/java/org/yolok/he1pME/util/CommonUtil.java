package org.yolok.he1pME.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class CommonUtil {
    public static JDA JDA;
    public static String SIGN;
    public static long FREQUENCY;
    public static Color HE1PME_COLOR = new Color(255, 192, 203);

    @Value("${sign}")
    private void setSign(String sign) {
        SIGN = sign;
    }

    @Value("${frequency}")
    private void setFrequency(long frequency) {
        FREQUENCY = frequency;
    }

    public static void replyByHe1pMETemplate(MessageChannel messageChannel, Member member,
                                             String title, String desc, String thumb) {
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(title).setDescription(desc).setColor(HE1PME_COLOR)
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
        if (StringUtils.isNotBlank(thumb)) {
            embedBuilder.setThumbnail(thumb);
        }
        messageChannel.sendMessageEmbeds(embedBuilder.build()).queue();
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
        if (CollectionUtils.isEmpty(source.getRoles()) || CollectionUtils.isEmpty(target.getRoles())) {
            return true;
        }

        List<Integer> sourcePositionList = source.getRoles().stream()
                .map(Role::getPosition)
                .sorted(Comparator.comparing(Integer::intValue).reversed())
                .toList();
        List<Integer> targetPositionList = target.getRoles().stream()
                .map(Role::getPosition)
                .sorted(Comparator.comparing(Integer::intValue).reversed())
                .toList();
        return sourcePositionList.get(0) <= targetPositionList.get(0) || target.getPermissions().contains(Permission.ADMINISTRATOR);
    }

    public static boolean notHigher(Member member, Role role) {
        if (CollectionUtils.isEmpty(member.getRoles())) {
            return true;
        }

        return member.getRoles().stream().noneMatch(eachRole -> eachRole.getPosition() > role.getPosition());
    }
}
