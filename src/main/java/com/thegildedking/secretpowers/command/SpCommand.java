package com.thegildedking.secretpowers.command;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.phase.Phase;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.team.Team;
import com.thegildedking.secretpowers.team.TeamManager;
import com.thegildedking.secretpowers.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SpCommand implements TabExecutor {

    private final SecretPowersPlugin plugin;

    public SpCommand(SecretPowersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "power" -> power(sender);
            case "team" -> team(sender, args);
            case "status" -> status(sender);
            case "admin" -> admin(sender, args);
            default -> Messages.send(sender, "<red>Unknown subcommand. Try <white>/sp help</white>.");
        }
        return true;
    }

    // ------------------------------------------------------------ help / status / power

    private void help(CommandSender s) {
        Messages.raw(s, "<gold><bold>SecretPowers commands");
        Messages.raw(s, "<yellow>/sp power <gray>- privately show your power");
        Messages.raw(s, "<yellow>/sp status <gray>- current phase and countdown");
        Messages.raw(s, "<yellow>/sp team <gray>- your team roster");
        Messages.raw(s, "<yellow>/sp team invite <player> <gray>- invite a player");
        Messages.raw(s, "<yellow>/sp team accept <player> <gray>- accept an invitation");
        Messages.raw(s, "<yellow>/sp team leave <gray>- leave your team");
        Messages.raw(s, "<yellow>/sp team share <gray>- tell your teammates your power");
        if (isAdmin(s)) {
            Messages.raw(s, "<red>/sp admin start | stop | reload");
            Messages.raw(s, "<red>/sp admin assign <player> <power>");
            Messages.raw(s, "<red>/sp admin reroll <player|all>");
            Messages.raw(s, "<red>/sp admin phase <idle|preparation|final|active>");
            Messages.raw(s, "<red>/sp admin team create|delete|add|remove|list");
        }
    }

    private void status(CommandSender s) {
        if (!s.hasPermission("secretpowers.use")) {
            deny(s);
            return;
        }
        Messages.send(s, "<yellow>Phase: <white>" + plugin.phases().describeStatus());
    }

    private void power(CommandSender s) {
        if (!(s instanceof Player player)) {
            Messages.send(s, "<red>Only players have powers.");
            return;
        }
        if (!player.hasPermission("secretpowers.use")) {
            deny(s);
            return;
        }
        if (!plugin.powers().hasPower(player.getUniqueId())) {
            Messages.send(player, "<gray>You do not have a power yet.");
            return;
        }
        plugin.powers().reveal(player);
        if (plugin.phases().getPhase() != Phase.IDLE) {
            plugin.activationItem().ensure(player); // hands the core back if it was lost
        }
    }

    // ------------------------------------------------------------ teams (players)

    private void team(CommandSender s, String[] args) {
        if (!(s instanceof Player player)) {
            Messages.send(s, "<red>Only players can use team commands.");
            return;
        }
        if (!player.hasPermission("secretpowers.team")) {
            deny(s);
            return;
        }
        if (args.length == 1) {
            showTeam(player);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "invite" -> invite(player, args);
            case "accept" -> accept(player, args);
            case "leave" -> leave(player);
            case "share" -> share(player);
            default -> Messages.send(player, "<gray>Usage: /sp team [invite <player> | accept <player> | leave | share]");
        }
    }

    private void showTeam(Player player) {
        Team team = plugin.teams().teamOf(player.getUniqueId());
        if (team == null) {
            Messages.send(player, "<gray>You are not in a team.");
            return;
        }
        Messages.raw(player, "<gold><bold>Team " + team.display());
        for (UUID id : team.members()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(id);
            String name = member.getName() == null ? id.toString().substring(0, 8) : member.getName();
            Messages.raw(player, (member.isOnline() ? "<green> - " : "<gray> - ") + name);
        }
    }

    private void invite(Player player, String[] args) {
        TeamManager teams = plugin.teams();
        if (teams.teamOf(player.getUniqueId()) == null) {
            Messages.send(player, "<red>You are not in a team.");
            return;
        }
        if (!plugin.getConfig().getBoolean("teams.allow-player-invites", true) && !isAdmin(player)) {
            Messages.send(player, "<red>Only administrators can add players to teams.");
            return;
        }
        if (args.length < 3) {
            Messages.send(player, "<gray>Usage: /sp team invite <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(player, "<red>That player is not online.");
            return;
        }
        if (target.equals(player)) {
            Messages.send(player, "<red>You cannot invite yourself.");
            return;
        }
        String error = teams.invite(player, target);
        if (error != null) {
            Messages.send(player, "<red>" + error);
        } else {
            Messages.send(player, "<green>Invitation sent to " + target.getName() + ".");
        }
    }

    private void accept(Player player, String[] args) {
        String inviterName = args.length >= 3 ? args[2] : null;
        String error = plugin.teams().accept(player, inviterName);
        if (error != null) {
            Messages.send(player, "<red>" + error);
            return;
        }
        Team team = plugin.teams().teamOf(player.getUniqueId());
        Messages.send(player, "<green>You joined team <white>" + (team == null ? "?" : team.display()) + "</white>.");
    }

    private void leave(Player player) {
        TeamManager teams = plugin.teams();
        Team team = teams.teamOf(player.getUniqueId());
        if (team == null) {
            Messages.send(player, "<red>You are not in a team.");
            return;
        }
        teams.notifyTeam(team, "<yellow>" + player.getName() + " left the team.", player.getUniqueId());
        teams.remove(player.getUniqueId());
        Messages.send(player, "<yellow>You left the team.");
    }

    /** Voluntary: the player chooses to tell their current teammates what their power is. */
    private void share(Player player) {
        PowerType type = plugin.powers().getAssigned(player.getUniqueId());
        if (type == null) {
            Messages.send(player, "<gray>You do not have a power yet.");
            return;
        }
        Team team = plugin.teams().teamOf(player.getUniqueId());
        if (team == null) {
            Messages.send(player, "<red>You are not in a team.");
            return;
        }
        int told = 0;
        for (UUID id : team.members()) {
            if (id.equals(player.getUniqueId())) continue;
            Player mate = Bukkit.getPlayer(id);
            if (mate == null) continue;
            Messages.send(mate, "<aqua>" + player.getName() + " shared their power with the team: <gold>"
                    + type.displayName() + "</gold> <gray>- " + type.description());
            told++;
        }
        Messages.send(player, "<green>Shared your power with " + told + " online teammate(s).");
    }

    // ------------------------------------------------------------ admin

    private void admin(CommandSender s, String[] args) {
        if (args.length < 2) {
            Messages.send(s, "<gray>Usage: /sp admin <start|stop|assign|reroll|phase|reload|team>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (!allowed(s, "start")) return;
                reply(s, plugin.phases().start(), "Preparation started.");
            }
            case "stop" -> {
                if (!allowed(s, "stop")) return;
                if (plugin.phases().getPhase() == Phase.IDLE) {
                    Messages.send(s, "<gray>No event is running.");
                    return;
                }
                plugin.phases().stop();
                Messages.send(s, "<green>Event stopped. Players keep their saved powers.");
            }
            case "assign" -> assign(s, args);
            case "reroll" -> reroll(s, args);
            case "phase" -> {
                if (!allowed(s, "phase")) return;
                if (args.length < 3) {
                    Messages.send(s, "<gray>Usage: /sp admin phase <idle|preparation|final|active>");
                    return;
                }
                reply(s, plugin.phases().setPhase(Phase.fromInput(args[2])), "Phase changed.");
            }
            case "reload" -> {
                if (!allowed(s, "reload")) return;
                plugin.reloadAll();
                Messages.send(s, "<green>Configuration reloaded.");
            }
            case "team" -> adminTeam(s, args);
            default -> Messages.send(s, "<red>Unknown admin subcommand.");
        }
    }

    private void assign(CommandSender s, String[] args) {
        if (!allowed(s, "assign")) return;
        if (args.length < 4) {
            Messages.send(s, "<gray>Usage: /sp admin assign <player> <power>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        PowerType type = PowerType.fromInput(args[3]);
        if (target == null) {
            Messages.send(s, "<red>That player is not online.");
            return;
        }
        if (type == null) {
            Messages.send(s, "<red>Unknown power. Options: " + String.join(", ", powerKeys()));
            return;
        }
        plugin.powers().set(target.getUniqueId(), type);
        plugin.powers().ensureAssigned(target, true);
        Messages.send(s, "<green>Assigned " + type.displayName() + " to " + target.getName() + ".");
    }

    private void reroll(CommandSender s, String[] args) {
        if (!allowed(s, "reroll")) return;
        if (args.length < 3) {
            Messages.send(s, "<gray>Usage: /sp admin reroll <player|all>");
            return;
        }
        if (args[2].equalsIgnoreCase("all")) {
            plugin.powers().rerollAll();
            Messages.send(s, "<green>Rerolled everyone. Online players were told privately.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(s, "<red>That player is not online.");
            return;
        }
        // The admin is deliberately not told the new power.
        if (plugin.powers().reroll(target)) {
            Messages.send(s, "<green>Rerolled " + target.getName() + ". They were told privately.");
        } else {
            Messages.send(s, "<red>No powers are enabled, so nothing could be assigned.");
        }
    }

    private void adminTeam(CommandSender s, String[] args) {
        if (!allowed(s, "teams")) return;
        TeamManager teams = plugin.teams();
        if (args.length < 3) {
            Messages.send(s, "<gray>Usage: /sp admin team <create|delete|add|remove|list> ...");
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 4) {
                    Messages.send(s, "<gray>Usage: /sp admin team create <name>");
                    return;
                }
                if (teams.create(args[3]) == null) {
                    Messages.send(s, "<red>Invalid or duplicate name (1-16 letters, numbers, - or _).");
                } else {
                    Messages.send(s, "<green>Team created.");
                }
            }
            case "delete" -> {
                if (args.length < 4) {
                    Messages.send(s, "<gray>Usage: /sp admin team delete <name>");
                    return;
                }
                Messages.send(s, teams.delete(args[3]) ? "<green>Team deleted." : "<red>No such team.");
            }
            case "add" -> {
                if (args.length < 5) {
                    Messages.send(s, "<gray>Usage: /sp admin team add <team> <player>");
                    return;
                }
                Team team = teams.get(args[3]);
                Player target = Bukkit.getPlayerExact(args[4]);
                if (team == null || target == null) {
                    Messages.send(s, "<red>Unknown team, or that player is not online.");
                    return;
                }
                String error = teams.add(team, target.getUniqueId());
                if (error != null) {
                    Messages.send(s, "<red>" + error);
                    return;
                }
                teams.notifyTeam(team, "<green>" + target.getName() + " joined the team.", target.getUniqueId());
                Messages.send(target, "<green>You were added to team <white>" + team.display() + "</white>.");
                Messages.send(s, "<green>Added " + target.getName() + " to " + team.display() + ".");
            }
            case "remove" -> {
                if (args.length < 4) {
                    Messages.send(s, "<gray>Usage: /sp admin team remove <player>");
                    return;
                }
                OfflinePlayer target = Bukkit.getPlayerExact(args[3]);
                if (target == null) target = Bukkit.getOfflinePlayerIfCached(args[3]);
                if (target == null || teams.teamOf(target.getUniqueId()) == null) {
                    Messages.send(s, "<red>That player is not in a team.");
                    return;
                }
                teams.remove(target.getUniqueId());
                Messages.send(s, "<green>Removed " + args[3] + " from their team.");
            }
            case "list" -> {
                if (teams.all().isEmpty()) {
                    Messages.send(s, "<gray>No teams exist.");
                    return;
                }
                for (Team team : teams.all()) {
                    List<String> names = new ArrayList<>();
                    for (UUID id : team.members()) {
                        String name = Bukkit.getOfflinePlayer(id).getName();
                        names.add(name == null ? id.toString().substring(0, 8) : name);
                    }
                    Messages.raw(s, "<yellow>" + team.display() + "<gray>: " + String.join(", ", names));
                }
            }
            default -> Messages.send(s, "<red>Unknown team subcommand.");
        }
    }

    // ------------------------------------------------------------ helpers

    private boolean isAdmin(CommandSender s) {
        if (s.hasPermission("secretpowers.admin")) return true;
        for (String sub : List.of("start", "stop", "assign", "reroll", "phase", "reload", "teams")) {
            if (s.hasPermission("secretpowers.admin." + sub)) return true;
        }
        return false;
    }

    private boolean allowed(CommandSender s, String sub) {
        if (s.hasPermission("secretpowers.admin") || s.hasPermission("secretpowers.admin." + sub)) return true;
        deny(s);
        return false;
    }

    private void deny(CommandSender s) {
        Messages.send(s, "<red>You do not have permission to do that.");
    }

    private void reply(CommandSender s, String error, String success) {
        if (error != null) {
            Messages.send(s, "<red>" + error);
        } else {
            Messages.send(s, "<green>" + success);
        }
    }

    private List<String> powerKeys() {
        List<String> keys = new ArrayList<>();
        for (PowerType t : PowerType.values()) keys.add(t.configKey());
        return keys;
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return names;
    }

    private List<String> teamNames() {
        List<String> names = new ArrayList<>();
        for (Team t : plugin.teams().all()) names.add(t.name());
        return names;
    }

    // ------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("help", "power", "team", "status"));
            if (isAdmin(sender)) options.add("admin");
            return match(args[0], options, out);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("team")) {
            if (args.length == 2) return match(args[1], List.of("invite", "accept", "leave", "share"), out);
            if (args.length == 3 && (args[1].equalsIgnoreCase("invite") || args[1].equalsIgnoreCase("accept"))) {
                return match(args[2], onlineNames(), out);
            }
            return out;
        }

        if (sub.equals("admin") && isAdmin(sender)) {
            if (args.length == 2) {
                return match(args[1], List.of("start", "stop", "assign", "reroll", "phase", "reload", "team"), out);
            }
            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "assign" -> {
                    if (args.length == 3) return match(args[2], onlineNames(), out);
                    if (args.length == 4) return match(args[3], powerKeys(), out);
                }
                case "reroll" -> {
                    if (args.length == 3) {
                        List<String> options = onlineNames();
                        options.add("all");
                        return match(args[2], options, out);
                    }
                }
                case "phase" -> {
                    if (args.length == 3) return match(args[2], Phase.inputNames(), out);
                }
                case "team" -> {
                    if (args.length == 3) {
                        return match(args[2], List.of("create", "delete", "add", "remove", "list"), out);
                    }
                    String teamSub = args[2].toLowerCase(Locale.ROOT);
                    if (teamSub.equals("delete") && args.length == 4) return match(args[3], teamNames(), out);
                    if (teamSub.equals("add")) {
                        if (args.length == 4) return match(args[3], teamNames(), out);
                        if (args.length == 5) return match(args[4], onlineNames(), out);
                    }
                    if (teamSub.equals("remove") && args.length == 4) return match(args[3], onlineNames(), out);
                }
                default -> { }
            }
        }
        return out;
    }

    private List<String> match(String token, Collection<String> options, List<String> out) {
        StringUtil.copyPartialMatches(token, options, out);
        return out;
    }
}
