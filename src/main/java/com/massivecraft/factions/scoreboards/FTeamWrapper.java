package com.massivecraft.factions.scoreboards;

import com.massivecraft.factions.*;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FTeamWrapper {

    /* Global FTeamWrapper variables */
    private static Map<String, FTeamWrapper> wrappers = new ConcurrentHashMap<>();
    private static Set<String> updating = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static Set<FScoreboard> tracking = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static Set<OfflinePlayer> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static int factionTeamPtr;

    /* FTeamWrapper instance variables*/
    private Map<FScoreboard, Team> teams = new ConcurrentHashMap<>();
    private String teamName;
    private Faction faction;

    public static void asyncPreloadTeamWrappers() {
        if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Faction faction : Factions.getInstance().getAllFactions()) {
                        wrappers.put(faction.getId(), new FTeamWrapper(faction));
                    }

                    P.p.debug("Preloaded FTeamWrappers for all factions. " + wrappers.size() + " in memory.");
                }
            }.runTaskAsynchronously(P.p);
        }
    }

    public static void applyUpdatesLater(final Faction faction) {
        if (!FScoreboard.isSupportedByServer() || !P.p.getConfig().getBoolean("scoreboard.default-prefixes", true)) {
            return;
        }

        if (updating.add(faction.getId())) {
            if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updating.remove(faction.getId());
                        update(faction);
                    }
                }.runTaskLaterAsynchronously(P.p, 10L);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updating.remove(faction.getId());
                        update(faction);
                    }
                }.runTaskLater(P.p, 10L);
            }
        }
    }

    public static void applyUpdates(final Faction faction) {
        if (!FScoreboard.isSupportedByServer() || !P.p.getConfig().getBoolean("scoreboard.default-prefixes", true)) {
            return;
        }

        if (updating.contains(faction.getId())) {
            return;  // Faction will be updated soon.
        }

        if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    update(faction);
                }
            }.runTaskAsynchronously(P.p);
        } else {
            update(faction);
        }
    }

    private static void update(Faction faction) {
        FTeamWrapper wrapper = wrappers.get(faction.getId());
        Set<FPlayer> factionMembers = faction.getFPlayers();
        boolean doWildernessUpdate = false;

        if (Factions.getInstance().getFactionById(faction.getId()) == null) {
            doWildernessUpdate = true; // update wilderness since it has a new player
        }

        if (wrapper == null) {
            P.p.debug("New FTeamWrapper for [" + faction.getTag() + "]");
            wrapper = new FTeamWrapper(faction);
            wrappers.put(faction.getId(), wrapper);
        }

        Set<OfflinePlayer> toRemove = new HashSet<>();

        for (OfflinePlayer player : members) {
            if (!player.isOnline()) {
                toRemove.add(player);
            }

            if (!factionMembers.contains(FPlayers.getInstance().getByOfflinePlayer(player))) {
                // player kicked or left, faction still exists, so update wilderness
                doWildernessUpdate = true;
                toRemove.add(player);
            }
        }

        for (OfflinePlayer player : toRemove) {
            wrapper.removePlayer(player);
        }

        if (doWildernessUpdate) {
            FTeamWrapper.applyUpdates(Factions.getInstance().getNone());
        }

        if (faction.isWilderness()) {
            // add faction-less players so we can format them in scoreboard
            for (FPlayer player : FPlayers.getInstance().getOnlinePlayers()) {
                if (!player.hasFaction()) {
                    wrapper.addPlayer(player.getPlayer());
                }
            }
        } else {
            for (FPlayer fmember : factionMembers) {
                if (!fmember.isOnline()) {
                    continue;
                }

                // Scoreboard might not have player; add him/her
                wrapper.addPlayer(fmember.getPlayer());
            }
        }

        wrapper.updatePrefixes();
    }

    public static void updatePrefixes(Faction faction) {
        if (!FScoreboard.isSupportedByServer()) {
            return;
        }

        if (!wrappers.containsKey(faction.getId())) {
            applyUpdates(faction);
        } else {
            wrappers.get(faction.getId()).updatePrefixes();
        }
    }

    protected static void track(FScoreboard fboard) {
        if (!FScoreboard.isSupportedByServer()) {
            return;
        }

        tracking.add(fboard);

        if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (FTeamWrapper wrapper : wrappers.values()) {
                        wrapper.add(fboard);
                    }
                }
            }.runTaskAsynchronously(P.p);
        } else {
            for (FTeamWrapper wrapper : wrappers.values()) {
                wrapper.add(fboard);
            }
        }
    }

    protected static void untrack(FScoreboard fboard) {
        if (!FScoreboard.isSupportedByServer()) {
            return;
        }

        tracking.remove(fboard);

        if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (FTeamWrapper wrapper : wrappers.values()) {
                        wrapper.remove(fboard);
                    }
                }
            }.runTaskAsynchronously(P.p);
        } else {
            for (FTeamWrapper wrapper : wrappers.values()) {
                wrapper.remove(fboard);
            }
        }
    }


    private FTeamWrapper(Faction faction) {
        this.teamName = "faction_" + (factionTeamPtr++);
        this.faction = faction;

        tracking.forEach(this::add);
    }

    private void add(FScoreboard fboard) {
        Scoreboard board = fboard.getScoreboard();
        Team team = board.getTeam(teamName);

        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        teams.put(fboard, team);

        for (OfflinePlayer player : members) {
            if (player.isOnline()) {
                team.addEntry(player.getName());
            }
        }

        updatePrefix(fboard);
    }

    private void remove(FScoreboard fboard) {
        teams.remove(fboard).unregister();
    }

    private void updatePrefixes() {
        if (P.p.getConfig().getBoolean("scoreboard.default-prefixes", true)) {
            if (P.p.getConfig().getBoolean("scoreboard.async-wrappers", false)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        teams.keySet().forEach(FTeamWrapper.this::updatePrefix);
                    }
                }.runTaskAsynchronously(P.p);
            } else {
                teams.keySet().forEach(this::updatePrefix);
            }
        }
    }

    private void updatePrefix(FScoreboard fboard) {
        if (P.p.getConfig().getBoolean("scoreboard.default-prefixes", true)) {
            FPlayer fplayer = fboard.getFPlayer();
            Team team = teams.get(fboard);

            String prefix = TL.DEFAULT_PREFIX.toString();

            prefix = prefix.replace("{relationcolor}", faction.getRelationTo(fplayer).getColor().toString());
            prefix = prefix.replace("{faction}", faction.getTag().substring(0, Math.min("{faction}".length() + 16 - prefix.length(), faction.getTag().length())));

            if (team.getPrefix() == null || !team.getPrefix().equals(prefix)) {
                team.setPrefix(prefix);
            }
        }
    }

    private void addPlayer(OfflinePlayer player) {
        if (members.add(player)) {
            for (Team team : teams.values()) {
                team.addEntry(player.getName());
            }
        }
    }

    private void removePlayer(OfflinePlayer player) {
        if (members.remove(player)) {
            for (Team team : teams.values()) {
                team.removeEntry(player.getName());
            }
        }
    }
}

