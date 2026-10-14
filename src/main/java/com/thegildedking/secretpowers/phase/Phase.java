package com.thegildedking.secretpowers.phase;

import java.util.List;
import java.util.Locale;

public enum Phase {
    IDLE("Idle"),
    PREPARATION("Preparation"),
    FINAL_COUNTDOWN("Final countdown"),
    ACTIVE("SMP active");

    private final String display;

    Phase(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public static Phase fromInput(String input) {
        if (input == null) return null;
        String s = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (s) {
            case "idle", "none", "off" -> IDLE;
            case "prep", "preparation" -> PREPARATION;
            case "final", "finalcountdown" -> FINAL_COUNTDOWN;
            case "active", "smp" -> ACTIVE;
            default -> null;
        };
    }

    public static List<String> inputNames() {
        return List.of("idle", "preparation", "final", "active");
    }
}
