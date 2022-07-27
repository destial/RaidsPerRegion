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
            case "time":
                return "" + Raid.minutes;
            case "left":
                return "" + Raid.mobsAlive;
        }
        return "0";
    }
}
