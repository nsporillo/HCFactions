package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CmdTop extends FCommand {

    public CmdTop() {
        super();
        this.aliases.add("top");
        this.aliases.add("t");

        this.requiredArgs.add("criteria");
        this.optionalArgs.put("page", "1");

        this.permission = Permission.TOP.node;
        this.disableOnLock = false;
        this.async = true; // This is a statistics cmd essentially, run async!

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        // Can sort by: money, members, online, allies, enemies, dtr, land.
        // Get all Factions and remove non player ones.
        final List<Faction> factionList = Factions.getInstance().getAllFactions();
        factionList.remove(Factions.getInstance().getNone());
        factionList.remove(Factions.getInstance().getSafeZone());
        factionList.remove(Factions.getInstance().getWarZone());

        String criteria = argAsString(0);

        if (criteria.equalsIgnoreCase("members")) {
            Collections.sort(factionList, (f1, f2) -> {
                int f1Size = f1.getFPlayers().size();
                int f2Size = f2.getFPlayers().size();

                if (f1Size < f2Size) {
                    return 1;
                } else if (f1Size > f2Size) {
                    return -1;
                }
                return 0;
            });
        } else if (criteria.equalsIgnoreCase("dtr")) {
            Collections.sort(factionList, (f1, f2) -> {
                double f1dtr = f1.getDTR();
                double f2dtr = f2.getDTR();
                if (f1dtr < f2dtr) {
                    return 1;
                } else if (f1dtr > f2dtr) {
                    return -1;
                }
                return 0;
            });
        } else if (criteria.equalsIgnoreCase("land")) {
            Collections.sort(factionList, (f1, f2) -> {
                int f1Size = f1.getLand();
                int f2Size = f2.getLand();
                if (f1Size < f2Size) {
                    return 1;
                } else if (f1Size > f2Size) {
                    return -1;
                }
                return 0;
            });
        } else if (criteria.equalsIgnoreCase("start")) {
            Collections.sort(factionList, (f1, f2) -> {
                long f1start = f1.getFoundedDate();
                long f2start = f2.getFoundedDate();
                // flip signs because a smaller date is further in the past
                if (f1start > f2start) {
                    return 1;
                } else if (f1start < f2start) {
                    return -1;
                }
                return 0;
            });
        } else if (criteria.equalsIgnoreCase("online")) {
            Collections.sort(factionList, (f1, f2) -> {
                int f1Size = f1.getFPlayersWhereOnline(true).size();
                int f2Size = f2.getFPlayersWhereOnline(true).size();
                if (f1Size < f2Size) {
                    return 1;
                } else if (f1Size > f2Size) {
                    return -1;
                }
                return 0;
            });
        } else if (Econ.isSetup() && criteria.equalsIgnoreCase("money") || criteria.equalsIgnoreCase("balance") || criteria.equalsIgnoreCase("bal")) {
            Collections.sort(factionList, (f1, f2) -> {
                double f1Size = Econ.getBalance(f1.getAccountId());
                for (FPlayer fp : f1.getFPlayers()) {
                    f1Size = f1Size + Econ.getBalance(fp);
                }

                double f2Size = Econ.getBalance(f2.getAccountId());
                for (FPlayer fp : f2.getFPlayers()) {
                    f2Size = f2Size + Econ.getBalance(fp);
                }

                if (f1Size < f2Size) {
                    return 1;
                } else if (f1Size > f2Size) {
                    return -1;
                }
                return 0;
            });
        } else {
            msg(TL.COMMAND_TOP_INVALID, criteria);
            return;
        }

        List<String> lines = new ArrayList<>();

        final int pageheight = 9;
        int pagenumber = this.argAsInt(1, 1);
        int pagecount = (factionList.size() / pageheight) + 1;

        if (pagenumber > pagecount) {
            pagenumber = pagecount;
        } else if (pagenumber < 1) {
            pagenumber = 1;
        }

        int start = (pagenumber - 1) * pageheight;
        int end = start + pageheight;
        if (end > factionList.size()) {
            end = factionList.size();
        }

        lines.add(TL.COMMAND_TOP_TOP.format(criteria.toUpperCase(), pagenumber, pagecount));

        int rank = 1;

        for (Faction faction : factionList.subList(start, end)) {
            // Get the relation color if player is executing this.
            String fac = sender instanceof Player ? faction.getRelationTo(fme).getColor() + faction.getTag() : faction.getTag();
            lines.add(TL.COMMAND_TOP_LINE.format(rank, fac, getValue(faction, criteria)));
            rank++;
        }

        sendMessage(lines);
    }

    private String getValue(Faction faction, String criteria) {
        if (criteria.equalsIgnoreCase("online")) {
            return String.valueOf(faction.getFPlayersWhereOnline(true).size());
        } else if (criteria.equalsIgnoreCase("members")) {
            return String.valueOf(faction.getFPlayers().size());
        } else if (criteria.equalsIgnoreCase("land")) {
            return String.valueOf(faction.getLand());
        } else if (criteria.equalsIgnoreCase("dtr")) {
            return TL.dc.format(faction.getDTR());
        } else if (criteria.equalsIgnoreCase("start")) {
            return TL.sdf.format(faction.getFoundedDate());
        } else { // Last one is balance, and it has 3 different things it could be.
            double balance = Econ.getBalance(faction.getAccountId());

            for (FPlayer fp : faction.getFPlayers()) {
                balance = balance + Econ.getBalance(fp);
            }

            return String.valueOf(balance);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_TOP_DESCRIPTION;
    }
}
