package me.ShermansWorld.RaidsPerRegion.commands;

import me.ShermansWorld.RaidsPerRegion.Main;
import me.ShermansWorld.RaidsPerRegion.Raid.Raid;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.color;
import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.getConfigBoolean;
import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.getConfigSection;
import static me.ShermansWorld.RaidsPerRegion.Raid.Raid.getConfigString;

public class RaidCommands implements CommandExecutor, TabExecutor {
	private final Main plugin;

	public RaidCommands(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return Collections.emptyList();
		}
		Player p = (Player) sender; // Convert sender into player
		World w = p.getWorld(); // Get world

		com.sk89q.worldedit.world.World bukkitWorld = BukkitAdapter.adapt(w);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(bukkitWorld);
		if (regions == null) return Collections.emptyList();

		Object[] regionHolder = regions.getRegions().keySet().toArray();

		List<String> completions = new ArrayList<>();

		if (args.length == 1 && sender.hasPermission("raidsperregion.raid")) {
			completions.add("region");
			completions.add("cancel");

			return completions;
		}

		if (args.length == 2 && sender.hasPermission("raidsperregion.raid") && args[0].equalsIgnoreCase("region")) {
			for (Object o : regionHolder) {
				completions.add(o.toString());
			}

			return completions;
		}

		if (args.length == 3 && sender.hasPermission("raidsperregion.raid") && ((args[0].equalsIgnoreCase("region")))) {
			completions.add("1");
			completions.add("2");
			completions.add("3");
			return completions;
		}

		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Check arguments

		// --------------------------------------------------------------------------------------------------------------

		if (!sender.hasPermission("raidsperregion.raid")) {
			sender.sendMessage(ChatColor.RED + "[RaidsPerRegion] You do not have permission to do this");
			return false;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
			if (Raid.region == null) { // if there is not a raid in progress
				sender.sendMessage("[RaidsPerRegion] There is not a raid in progress right now");
			} else {
				sender.sendMessage("[RaidsPerRegion] Canceled raid on region " + Raid.region.getId());
				Main.cancelledRaid = true;
				if (Raid.isScheduled) {
					plugin.getServer().getScheduler().cancelTasks(plugin); // cancel all tasks including schedule delay
					Raid.region = null;
				}
			}
			return false;
		}

		if (args.length < 3 || args.length > 4) {
			sender.sendMessage("[RaidsPerRegion] Invalid arguments");
			sender.sendMessage("[RaidsPerRegion] Usage: /raid region [region] [tier]");
			return false;
		}

		if (Raid.region != null) {
			sender.sendMessage("[RaidsPerRegion] There is already a raid in progress in region " + Raid.region.getId());
			sender.sendMessage("[RaidsPerRegion] To cancel this raid type /raid cancel");
			return false;
		}

		if (args[0].equalsIgnoreCase("region")) {
			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer(); // Put regions on

			for (World world : plugin.getServer().getWorlds()) {
				RegionManager regions = container.get(BukkitAdapter.adapt(world));
				if (regions == null) continue;
				Raid.region = regions.getRegion(args[1]);
				if (Raid.region != null) break;
			}

			if (Raid.region == null) {
				sender.sendMessage("[RaidsPerRegion] Invalid region. Usage: /raid region [region] [tier]");
				return false;
			}
		}

		if (args[2].contentEquals("1") || args[2].contentEquals("2") || args[2].contentEquals("3")) {
			Raid.tier = Integer.parseInt(args[2]);
			Raid.goal = getConfigSection("Tier" + Raid.tier).getInt("KillsGoal");
			Raid.countdown = getConfigSection("Tier" + Raid.tier).getInt("Time");
			Raid.maxMobsPerPlayer = getConfigSection("Tier" + Raid.tier).getInt("MaxMobsPerPlayer");
			Raid.spawnRateMultiplier = getConfigSection("Tier" + Raid.tier).getDouble("SpawnRateMultiplier");
			Raid.conversionSpawnRateMultiplier = (long) Raid.spawnRateMultiplier;
			Raid.mobLevel = getConfigSection("Tier" + Raid.tier).getInt("MobLevel");
			if (getConfigBoolean("SpawnBossOnKillGoalReached")) {
				Raid.boss = getConfigSection("Tier" + Raid.tier).getString("Boss");
			} else {
				Raid.boss = "NONE";
			}
			if (Raid.conversionSpawnRateMultiplier == 0) {
				Raid.conversionSpawnRateMultiplier = 1;
				sender.sendMessage("[RaidsPerRegion] SpawnRateMultipiler too low! Defaulting to 1.0");
			}

		} else {
			sender.sendMessage("[RaidsPerRegion] Invalid tier. Usage: /raid region [region] [tier]");
			return false;
		}
		
		int delayRaidTicks = 20;
		
		if (args.length == 4) {
			int scheduledMins;
			try {
		        scheduledMins = Integer.parseInt(args[3]);
		    } catch (NumberFormatException nfe) {
		    	sender.sendMessage("Invalid scheduled time. Usage: /raid region [region] [tier] {Time in mins}");
		        return false;
		    }
			
			delayRaidTicks = 1200 * scheduledMins; // 1200 ticks per min
			
			Raid.isScheduled = true;

			if (Raid.region != null) {
				sender.sendMessage("[RaidsPerRegion] Raid scheduled for " + Raid.region.getId() + " in " + args[3] + " minutes");
			}
		}

		// -----------------------------------------------------------------------------------------------------------------------

		// delay for scheduled task
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
			Raid.resetVariables();
			if (Raid.region != null) {
				if (Raid.region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
					Raid.hasMobsOn = true;
				} else {
					Raid.hasMobsOn = false;
					Raid.region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.ALLOW);
				}
			}

			Raid.minutes = Raid.countdown / 60;
			Raid.countdown = Raid.countdown % 60;

			Raid.getMobsFromConfig();

			if (Raid.region != null) {
				Raid.checkPlayersInRegion();
			}

			int[] id = { 0 };
			id[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
				if (!Raid.playersInRegion.isEmpty() && !Raid.runOnce) {
					Raid.runOnce = true;
					String raidAnnoucementTitle = getConfigString("RaidAnnoucementTitle");
					String raidAnnoucementSubtitle = getConfigString("RaidAnnoucementSubtitle");
					if (raidAnnoucementTitle.contains("@TIER")) {
						raidAnnoucementTitle = raidAnnoucementTitle.replaceAll("@TIER", args[2]);
					}
					if (raidAnnoucementSubtitle.contains("@TIER")) {
						raidAnnoucementSubtitle = raidAnnoucementSubtitle.replaceAll("@TIER", args[2]);
					}
					if (Raid.region != null) {
						if (raidAnnoucementTitle.contains("@REGION")) {
							raidAnnoucementTitle = raidAnnoucementTitle.replaceAll("@REGION", Raid.region.getId());
						}
						if (raidAnnoucementSubtitle.contains("@REGION")) {
							raidAnnoucementSubtitle = raidAnnoucementSubtitle.replaceAll("@REGION", Raid.region.getId());
						}
					}
					if (raidAnnoucementTitle.contains("@SENDER")) {
						raidAnnoucementTitle = raidAnnoucementTitle.replaceAll("@SENDER", sender.getName());
					}
					if (raidAnnoucementSubtitle.contains("@SENDER")) {
						raidAnnoucementSubtitle = raidAnnoucementSubtitle.replaceAll("@SENDER", sender.getName());
					}
					for (Player player : Raid.playersInRegion) {
						player.sendTitle(color(raidAnnoucementTitle), color(raidAnnoucementSubtitle), 10, 60, 10);
					}
				}

				if (Raid.isCancelledRaid(args[2], sender) || Raid.isWonRaid(args[2], Raid.goal, Raid.boss, Raid.mobLevel, sender) || Raid.isLostRaid(args[2], Raid.goal, Raid.minutes, sender)) {
					plugin.getServer().getScheduler().cancelTask(id[0]);
					Raid.timeReached = true;
					Raid.boss = "NONE";
					Raid.mobLevel = 1;

					if (Raid.region != null) {
						if (!Raid.hasMobsOn) {
							Raid.region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
						}
						Raid.region = null;
					}
					return;
				}

				if (Raid.countdown == 0 && Raid.minutes >= 1) {
					Raid.minutes--;
					Raid.countdown += 60;
				}

				Raid.countdown--;

			}, 0L, 20L); // repeats every second
		}, delayRaidTicks); // delay for scheduled raids

		return false;
	}

}
