package me.ShermansWorld.RaidsPerRegion.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.ShermansWorld.RaidsPerRegion.Main;
import me.ShermansWorld.RaidsPerRegion.Raid.Raid;
import net.md_5.bungee.api.ChatColor;

import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.color;
import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.getConfigString;

public final class MobListener implements Listener {
	
	@EventHandler
	public void onMythicMobDead(MythicMobDeathEvent event) {
		if (Raid.region != null) { // if a raid is happening
			AbstractEntity mobEntity = event.getMob().getEntity();
			if (Raid.MmEntityList.contains(mobEntity)) {
				Raid.mobsLeft--;
				LivingEntity killer = event.getKiller();
				if (killer instanceof Player) {
					Player player = (Player) killer;
					Raid.raidKills.putIfAbsent(player.getName(), 0);
					Raid.raidKills.put(player.getName(), Raid.raidKills.get(player.getName()) + 1); // if already mapped, set kills to kills + 1
					if (Raid.bossSpawned) {
						if (mobEntity.equals(Raid.bossEntity)) {
							Raid.boss = "NONE"; // should end the raid
							String killmsg = getConfigString("BossKilledMessage");
							if (killmsg.contentEquals("")) {
								return;
							}
							if (killmsg.contains("@PLAYER")) {
								killmsg = killmsg.replaceAll("@PLAYER", player.getName());
							}
							if (killmsg.contains("@TIER")) {
								killmsg = killmsg.replaceAll("@TIER", "" + Raid.tier);
							}
							if (killmsg.contains("@BOSSNAME")) {
								killmsg = killmsg.replaceAll("@BOSSNAME", event.getMob().getDisplayName());
							}
							if (killmsg.contains("@REGION")) {
								if (Raid.region != null) {
									killmsg = killmsg.replaceAll("@REGION", Raid.region.getId());
								}
							}
							Bukkit.broadcastMessage(color(killmsg));
						}
					}
				} else {
					Raid.otherDeaths++;
					if (mobEntity.equals(Raid.bossEntity)) {
						Raid.boss = "NONE"; // should end the raid
					}
				}
			}
			
		}
	}
}


