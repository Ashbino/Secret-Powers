package com.thegildedking.secretpowers.power;

import java.util.Locale;

public enum PowerType {
    PHANTOM("phantom", "Phantom", "Teleport a short distance in the direction you are looking.", false),
    JUGGERNAUT("juggernaut", "Juggernaut", "Gain heavy damage resistance for a few seconds, but move slower.", false),
    STORMCALLER("stormcaller", "Stormcaller", "Call down lightning where you are looking.", false),
    GHOST("ghost", "Ghost", "Turn invisible for a few seconds. Attacking breaks the effect.", false),
    LIFEBINDER("lifebinder", "Lifebinder", "Heal yourself or a nearby teammate.", false),
    SKYBOUND("skybound", "Skybound", "Leap high into the air.", false),
    SOUL_THIEF("soulthief", "Soul Thief", "Your melee hits steal experience from other players.", true),
    FROSTBITE("frostbite", "Frostbite", "Slow the enemy you are looking at.", false),
    INFERNO("inferno", "Inferno", "Become fire resistant and scorch nearby enemies.", false),
    FORCEWAVE("forcewave", "Forcewave", "Blast nearby players away from you.", false);

    private final String configKey;
    private final String displayName;
    private final String description;
    private final boolean passive;

    PowerType(String configKey, String displayName, String description, boolean passive) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.description = description;
        this.passive = passive;
    }

    public String configKey() { return configKey; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public boolean passive() { return passive; }

    public String activationText() {
        return passive
                ? "Passive - just hit other players in melee. There is nothing to activate."
                : "Hold your Power Core and right-click.";
    }

    public static PowerType fromInput(String input) {
        if (input == null) return null;
        String s = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        for (PowerType t : values()) {
            if (t.configKey.equals(s)) return t;
        }
        return null;
    }
}
