package com.thegildedking.secretpowers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gold>[SecretPowers]</gold> ";

    private Messages() {}

    public static Component parse(String mini) {
        return MM.deserialize(mini);
    }

    public static Component itemName(String mini) {
        return MM.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender to, String mini) {
        to.sendMessage(MM.deserialize(PREFIX + mini));
    }

    public static void raw(CommandSender to, String mini) {
        to.sendMessage(MM.deserialize(mini));
    }

    public static void actionBar(Player to, String mini) {
        to.sendActionBar(MM.deserialize(mini));
    }

    public static void broadcast(String mini) {
        Bukkit.broadcast(MM.deserialize(PREFIX + mini));
    }

    public static void titleAll(String main, String sub) {
        Title title = Title.title(
                MM.deserialize(main),
                MM.deserialize(sub),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(300)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    public static String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
