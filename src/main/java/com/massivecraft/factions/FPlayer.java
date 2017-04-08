package com.massivecraft.factions;

import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.ChatMode;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;


/**
 * Logged in players always have exactly one FPlayer instance. Logged out players may or may not have an FPlayer
 * instance. They will always have one if they are part of a faction. This is because only players with a faction are
 * saved to disk (in order to not waste disk space).
 * <p/>
 * The FPlayer is linked to a minecraft player using the player name.
 * <p/>
 * The same instance is always returned for the same player. This means you can use the == operator. No .equals method
 * necessary.
 */

public interface FPlayer extends EconomyParticipator {
    Faction getFaction();

    String getFactionId();

    boolean hasFaction();

    void setFaction(Faction faction);

    void setMonitoringJoins(boolean monitor);

    boolean isMonitoringJoins();

    Role getRole();

    void setRole(Role role);

    Faction getAutoClaimFor();

    void setAutoClaimFor(Faction faction);

    boolean isAutoSafeClaimEnabled();

    void setIsAutoSafeClaimEnabled(boolean enabled);

    boolean isAutoWarClaimEnabled();

    void setIsAutoWarClaimEnabled(boolean enabled);

    boolean isAdminBypassing();

    boolean isVanished(FPlayer player);

    void setIsAdminBypassing(boolean val);

    void setChatMode(ChatMode chatMode);

    ChatMode getChatMode();

    void setSpyingChat(boolean chatSpying);

    boolean isSpyingChat();

    // FIELD: account
    String getAccountId();

    void resetFactionData(boolean doSpoutUpdate);

    void resetFactionData();

    long getLastLoginTime();

    long getLastCombatTime();

    void setLastLoginTime(long lastLoginTime);

    void setLastCombatTime(long lastCombatTime);

    boolean isMapAutoUpdating();

    void setMapAutoUpdating(boolean mapAutoUpdating);

    boolean hasLoginPvpDisabled();

    FLocation getLastStoodAt();

    void setLastStoodAt(FLocation flocation);

    String getTitle();

    void setTitle(String title);

    String getName();

    String getTag();

    int getKills();

    int getDeaths();

    // Base concatenations:

    String getNameAndSomething(String something);

    String getNameAndTitle();

    String getNameAndTag();

    // Colored concatenations:
    // These are used in information messages

    String getNameAndTitle(Faction faction);

    String getNameAndTitle(FPlayer fplayer);

    // Chat Tag:
    // These are injected into the format of global chat messages.

    String getChatTag();

    // Colored Chat Tag
    String getChatTag(Faction faction);

    String getChatTag(FPlayer fplayer);

    // -------------------------------
    // Relation and relation colors
    // -------------------------------

    @Override
    String describeTo(RelationParticipator that, boolean ucfirst);

    @Override
    String describeTo(RelationParticipator that);

    @Override
    Relation getRelationTo(RelationParticipator rp);

    @Override
    Relation getRelationTo(RelationParticipator rp, boolean ignorePeaceful);

    Relation getRelationToLocation();

    @Override
    ChatColor getColorTo(RelationParticipator rp);

    //----------------------------------------------//
    // Health
    //----------------------------------------------//
    void heal(int amnt);

    void onDeath(String world);

    //----------------------------------------------//
    // Territory
    //----------------------------------------------//
    boolean isInOwnTerritory();

    boolean isInOthersTerritory();

    boolean isInAllyTerritory();

    boolean isInNeutralTerritory();

    boolean isInEnemyTerritory();

    void sendFactionHereMessage(Faction from);

    /**
     * Check if the scoreboard should be shown. Simple method to be used by above method.
     *
     * @param toShow Faction to be shown.
     * @return true if should show, otherwise false.
     */
    boolean shouldShowScoreboard(Faction toShow);

    // -------------------------------
    // Actions
    // -------------------------------

    void leave(boolean makePay);

    boolean canClaimForFaction(Faction forFaction);

    boolean canClaimForFactionAtLocation(Faction forFaction, Location location, boolean notifyFailure);

    boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure);

    boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure, boolean notifySuccess);

    void msg(String str, Object... args);

    String getId();

    Player getPlayer();

    boolean isOnline();

    void sendMessage(String message);

    void sendMessage(List<String> messages);

    boolean isOnlineAndVisibleTo(Player me);

    void remove();

    boolean isOffline();

    void setId(String id);
}
