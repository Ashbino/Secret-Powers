package com.thegildedking.secretpowers.team;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.util.Messages;
import com.thegildedking.secretpowers.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamManager {

    private record Invite(UUID inviter, String team, long expiresAt) {}

    private final SecretPowersPlugin plugin;
    private final File file;
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private boolean dirty;

    public TeamManager(SecretPowersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
    }

    // ------------------------------------------------------------ persistence

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = y.getConfigurationSection("teams");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Team team = new Team(key, section.getString(key + ".display", key));
            for (String raw : section.getStringList(key + ".members")) {
                try {
                    UUID id = UUID.fromString(raw);
                    if (membership.containsKey(id)) continue; // a player can only be in one team
                    team.members().add(id);
                    membership.put(id, key);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignoring invalid member UUID in teams.yml: " + raw);
                }
            }
            teams.put(key, team);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Team team : teams.values()) {
            String base = "teams." + team.name();
            y.set(base + ".display", team.display());
            List<String> ids = new ArrayList<>();
            for (UUID id : team.members()) ids.add(id.toString());
            y.set(base + ".members", ids);
        }
        YamlStore.save(y, file, plugin.getLogger());
        dirty = false;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    // ------------------------------------------------------------ queries

    public Collection<Team> all() {
        return teams.values();
    }

    public Team get(String name) {
        return name == null ? null : teams.get(name.toLowerCase(Locale.ROOT));
    }

    public Team teamOf(UUID id) {
        String name = membership.get(id);
        return name == null ? null : teams.get(name);
    }

    public boolean areTeammates(UUID a, UUID b) {
        if (a.equals(b)) return false;
        String ta = membership.get(a);
        return ta != null && ta.equals(membership.get(b));
    }

    // ------------------------------------------------------------ admin operations

    public Team create(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (!key.matches("[a-z0-9_-]{1,16}") || teams.containsKey(key)) return null;
        Team team = new Team(key, name);
        teams.put(key, team);
        dirty = true;
        saveIfDirty();
        return team;
    }

    public boolean delete(String name) {
        Team team = teams.remove(name.toLowerCase(Locale.ROOT));
        if (team == null) return false;
        for (UUID id : team.members()) membership.remove(id);
        dirty = true;
        saveIfDirty();
        return true;
    }

    /** Returns an error message, or null on success. Moving between teams removes the old membership. */
    public String add(Team team, UUID id) {
        if (team.name().equals(membership.get(id))) return "Already in that team.";
        int max = plugin.getConfig().getInt("teams.max-size", 4);
        if (team.members().size() >= max) return "That team is full (max " + max + ").";
        remove(id);
        team.members().add(id);
        membership.put(id, team.name());
        invites.remove(id);
        dirty = true;
        saveIfDirty();
        return null;
    }

    public void remove(UUID id) {
        String name = membership.remove(id);
        if (name == null) return;
        Team team = teams.get(name);
        if (team != null) team.members().remove(id);
        dirty = true;
        saveIfDirty();
    }

    // ------------------------------------------------------------ player invites

    /** Returns an error message, or null on success. */
    public String invite(Player inviter, Player target) {
        Team team = teamOf(inviter.getUniqueId());
        if (team == null) return "You are not in a team.";
        if (teamOf(target.getUniqueId()) != null) return target.getName() + " is already in a team.";
        int max = plugin.getConfig().getInt("teams.max-size", 4);
        if (team.members().size() >= max) return "Your team is full (max " + max + ").";

        long expires = System.currentTimeMillis()
                + plugin.getConfig().getInt("teams.invite-expire-seconds", 60) * 1000L;
        invites.put(target.getUniqueId(), new Invite(inviter.getUniqueId(), team.name(), expires));
        Messages.send(target, "<green>" + inviter.getName() + " invited you to team <white>" + team.display()
                + "</white>. Type <white>/sp team accept " + inviter.getName() + "</white> to join.");
        return null;
    }

    /** Returns an error message, or null on success. */
    public String accept(Player player, String inviterName) {
        Invite invite = invites.get(player.getUniqueId());
        if (invite == null || invite.expiresAt() < System.currentTimeMillis()) {
            invites.remove(player.getUniqueId());
            return "You have no pending invitation.";
        }
        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviterName != null && (inviter == null || !inviter.getName().equalsIgnoreCase(inviterName))) {
            return "You have no invitation from that player.";
        }
        Team team = teams.get(invite.team());
        if (team == null) return "That team no longer exists.";
        if (teamOf(player.getUniqueId()) != null) return "Leave your current team first.";

        String error = add(team, player.getUniqueId());
        if (error != null) return error;
        notifyTeam(team, "<green>" + player.getName() + " joined the team.", player.getUniqueId());
        return null;
    }

    public void notifyTeam(Team team, String mini, UUID except) {
        for (UUID id : team.members()) {
            if (id.equals(except)) continue;
            Player member = Bukkit.getPlayer(id);
            if (member != null) Messages.send(member, mini);
        }
    }
}
