package me.ShermansWorld.RaidsPerRegion.papi;

import me.ShermansWorld.RaidsPerRegion.Main;
import me.ShermansWorld.RaidsPerRegion.Raid.Raid;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PAPIHook extends PlaceholderExpansion {
    public PAPIHook() {
        super();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "raidsperregion";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return Main.getInstance().getDescription().getAuthors().get(0);
    }

    @NotNull
    @Override
    public String getVersion() {
        return Main.getInstance().getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params) {
            case "kills": {
                int score = Raid.raidKills.getOrDefault(player.getName(), 0);
                return "" + score;
            }
            case "goal" : return "" + Raid.goal;
            case "total": return "" + Raid.totalKills;
            case "time": return "" + (Raid.minutes > 10 ? "0" + Raid.minutes : Raid.minutes) + ":" + (Raid.countdown > 10 ? "0" + Raid.countdown : Raid.countdown);
            case "left": return "" + Raid.mobsLeft;
            default: break;
        }
        return "0";
    }
}
