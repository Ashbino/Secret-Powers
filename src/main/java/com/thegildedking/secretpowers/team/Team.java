package com.thegildedking.secretpowers.team;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Team {

    private final String name;
    private final String display;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Team(String name, String display) {
        this.name = name;
        this.display = display;
    }

    public String name() { return name; }
    public String display() { return display; }
    public Set<UUID> members() { return members; }
}
