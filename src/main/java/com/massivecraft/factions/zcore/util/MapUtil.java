package com.massivecraft.factions.zcore.util;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class MapUtil {

    private final static List<UUID> enabledMap = new ArrayList<>();
    private final static Map<UUID, CornerGrid> fakePillars = new HashMap<>();

    private static int maxHeight = 256;

    public static boolean toggleEnabled(UUID uuid) {
        if (enabledMap.contains(uuid)) {
            enabledMap.remove(uuid);
        } else {
            enabledMap.add(uuid);
        }

        return enabledMap.contains(uuid);
    }

    public static void updatePlayer(Player player, FLocation location, Faction faction) {
        final Board board = Board.getInstance();

        if (enabledMap.contains(player.getUniqueId())) {
            sendFactionBorder(player, board.getFactionAt(location.getRelative(0, 1)));
            sendFactionBorder(player, board.getFactionAt(location.getRelative(0, -1)));
            sendFactionBorder(player, board.getFactionAt(location.getRelative(1, 0)));
            sendFactionBorder(player, board.getFactionAt(location.getRelative(1, 1)));
        }
    }

    public static void sendFactionBorder(Player player, Faction faction) {
        String world = player.getWorld().getName();

        for (FLocation flocation : faction.getAllClaims()) {
            // Don't send claim border updates for chunks in other worlds
            if (flocation.getWorldName().equals(world)) {
                sendChunkPillars(player, flocation);
            }
        }
    }

    private static void sendChunkPillars(Player player, FLocation fLocation) {
        int chunkX = (int) fLocation.getX();
        int chunkZ = (int) fLocation.getZ();

        showPillar(player, chunkX * 16, chunkZ * 16);
        showPillar(player, chunkX * 16 + 15, chunkZ * 16);
        showPillar(player, chunkX * 16, chunkZ * 16 + 15);
        showPillar(player, chunkX * 16 + 15, chunkZ * 16 + 15);
    }

    private static void showPillar(Player player, int x, int z) {
        Set<Location> newPillar;

        /**
         * If a corner grid exists, add a new pillar
         * otherwise, create a new grid which auto creates the first pillar
         */
        if (fakePillars.containsKey(player.getUniqueId())) {
            CornerGrid cornerGrid = fakePillars.get(player.getUniqueId());
            newPillar = cornerGrid.addPillar(player.getWorld(), x, z);
            fakePillars.put(player.getUniqueId(), cornerGrid);
        } else {
            CornerGrid grid = new CornerGrid(player.getWorld(), x, z);
            newPillar = grid.getPillarBlocks();
            fakePillars.put(player.getUniqueId(), grid);
        }

        for (Location loc : newPillar) {
            Block block = loc.getBlock();

            if (block.isEmpty()) {
                player.sendBlockChange(loc, Material.GLASS, (byte) 0);
            }
        }
    }

    private static void hidePillars(Player player) {
        if (fakePillars.containsKey(player.getUniqueId())) {
            CornerGrid pillar = fakePillars.get(player.getUniqueId());

            for (Location loc : pillar.getPillarBlocks()) {
                loc.getBlock().setType(Material.AIR);
            }

            fakePillars.remove(player.getUniqueId());
        }
    }

    @Data
    private static class CornerGrid {

        private Set<Location> pillarBlocks = new HashSet<>();

        public CornerGrid(World world, int x, int z) {
            addPillar(world, x, z);
        }

        public Set<Location> addPillar(World world, int x, int z) {
            Set<Location> newPillar = new HashSet<>();

            for (int y = 0; y < maxHeight; y++) {
                newPillar.add(new Location(world, x, y, z));
            }

            pillarBlocks.addAll(newPillar);
            return newPillar;
        }
    }
}
