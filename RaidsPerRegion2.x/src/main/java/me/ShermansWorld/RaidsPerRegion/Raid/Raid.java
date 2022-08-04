package me.ShermansWorld.RaidsPerRegion.Raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.ShermansWorld.RaidsPerRegion.Main;

public class Raid {
	// Raid command variables
	public static boolean timeReached = false;
	public static int totalKills;
	public static boolean maxMobsReached = false;
	public static List<String> mMMobNames = new ArrayList<>();
	public static List<Double> chances = new ArrayList<>();
	public static List<Integer> priorities = new ArrayList<>();
	public static boolean runOnce = false;
	public static int countdown;
	public static int minutes;
	public static boolean hasMobsOn;
	public static boolean isScheduled = false;

	// public variables used in Listener Class
	public static List<Player> playersInRegion = new ArrayList<>();
	public static Map<String, Integer> raidKills = new HashMap<>();
	public static int otherDeaths = 0;
	public static List<AbstractEntity> MmEntityList = new ArrayList<>();
	public static int mobsSpawned = 0;
	public static boolean bossSpawned = false;
	public static String boss = "NONE";
	public static int mobLevel = 1;
	public static AbstractEntity bossEntity;
	public static int tier = 1;

	// regions and towns
	public static ProtectedRegion region;

	// other (previous local) - Initial values for tiers from config
	public static int goal;
	public static int maxMobsPerPlayer = 10;
	public static double spawnRateMultiplier = 1.0;
	public static long conversionSpawnRateMultiplier = 10;
	public static int mobsAlive;
	public static int mobsLeft;

	public static String color(String s) {
		return ChatColor.translateAlternateColorCodes('&',s);
	}

	// resetVariables Method
	public static void resetVariables() {
		Raid.timeReached = false;
		Raid.totalKills = 0;
		Raid.mobsSpawned = 0;
		Raid.maxMobsReached = false;
		Raid.playersInRegion = new ArrayList<>();
		Raid.MmEntityList = new ArrayList<>();
		Raid.mMMobNames = new ArrayList<>();
		Raid.raidKills = new HashMap<>();
		Main.cancelledRaid = false;
		Raid.runOnce = false;
		Raid.priorities = new ArrayList<>();
		Raid.chances = new ArrayList<>();
		Raid.mMMobNames = new ArrayList<>();
		Raid.otherDeaths = 0;
		Raid.bossSpawned = false;
		Raid.isScheduled = false;
		Raid.mobsAlive = 0;
		Raid.mobsLeft = 0;
	}

	public static String getConfigString(String path) {
		return Main.getInstance().getConfig().getString(path, "");
	}

	public static boolean getConfigBoolean(String path) {
		return Main.getInstance().getConfig().getBoolean(path, false);
	}

	public static ConfigurationSection getConfigSection(String path) {
		return Main.getInstance().getConfig().getConfigurationSection(path);
	}

	public static List<String> getConfigStringList(String path) {
		return Main.getInstance().getConfig().getStringList(path);
	}

	// spawnMobs Method
	public static void spawnMobs(Random rand, List<Location> regionPlayerLocations, int scoreCounter, List<String> mMMobNames, List<Double> chances, List<Integer> priorities, int maxMobsPerPlayer, int mobLevel) {
		for (Player player : Raid.playersInRegion) {
			int randomPlayerIdx = rand.nextInt(Raid.playersInRegion.size());
			World w = player.getWorld();
			int x = regionPlayerLocations.get(randomPlayerIdx).getBlockX() + rand.nextInt(50) - 25;
			int y = regionPlayerLocations.get(randomPlayerIdx).getBlockY();
			int z = regionPlayerLocations.get(randomPlayerIdx).getBlockZ() + rand.nextInt(50) - 25;
			String mythicMobName;
			int spawnRate = rand.nextInt(3); // 1/3 chance of spawning zombie at this player per cycle. possibilities:
			// 0, 1 or 2
			int numPlayersInRegion = Raid.playersInRegion.size();
			Raid.mobsAlive = Raid.mobsSpawned - scoreCounter - Raid.otherDeaths;
			Raid.maxMobsReached = mobsAlive >= numPlayersInRegion * maxMobsPerPlayer;

			if (spawnRate == 2 && !Raid.maxMobsReached) {
				List<Integer> hitIdxs = new ArrayList<>();
				for (int k = 0; k < mMMobNames.size(); k++) {
					int randomNum = rand.nextInt(1000) + 1; // generates number between 1 and 1000
					if (randomNum <= chances.get(k) * 1000) { // test for hit
						hitIdxs.add(k); // add hit index
					}
				}
				int maxPriority = 0;
				int maxPriorityIdx = 0;
				for (Integer hitIdx : hitIdxs) {
					if (priorities.get(hitIdx) > maxPriority) { // does not account for same priority
						maxPriority = priorities.get(hitIdx);
						maxPriorityIdx = hitIdx;
					}
				}

				mythicMobName = mMMobNames.get(maxPriorityIdx); // set to first idx if nothing else hits
				// Fix mob spawning in ground or in air and taking fall damage
				if (w.getBlockAt(x, y, z).getType() == Material.AIR) {
					while (w.getBlockAt(x, y, z).getType() == Material.AIR) {
						y--;
					}
					y += 2; // fixes mobs spawning half in a block
				} else {
					while (w.getBlockAt(x, y, z).getType() != Material.AIR) {
						y++;
					}
					y++; // fixes mobs spawning half in a block
				}

				Location mobSpawnLocation = new Location(w, x, y, z);
				boolean regionOnly = getConfigBoolean("MobsSpawnOnlyInRegion");
				if (regionOnly && Raid.region != null) {
					if (!Raid.region.contains(mobSpawnLocation.getBlockX(), mobSpawnLocation.getBlockY(), mobSpawnLocation.getBlockZ())) {
						spawnMobs(rand, regionPlayerLocations, maxPriorityIdx, mMMobNames, chances, hitIdxs, maxPriorityIdx, maxPriorityIdx);
						return;
					}
				}

				ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(mythicMobName, mobSpawnLocation, mobLevel);
				try {
					AbstractEntity entityOfMob = mob.getEntity();
					Raid.MmEntityList.add(entityOfMob);
					Raid.mobsSpawned++;
					Raid.mobsLeft++;
				} catch (NullPointerException ignored) {}
			}
			Raid.totalKills = scoreCounter;
		}
	}

	public static void checkPlayersInRegion() {
		Random rand = new Random();
		int[] id = { 0 };
		id[0] = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), () -> {
			if (Raid.timeReached) {
				Bukkit.getServer().getScheduler().cancelTask(id[0]);
			} else {
				Raid.playersInRegion = new ArrayList<>();
				List<Location> regionPlayerLocations = new ArrayList<>();
				int scoreCounter = 0;

				for (Player player : Bukkit.getOnlinePlayers()) {
					Location location = player.getLocation();
					if (Raid.region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
						Raid.playersInRegion.add(player);
						regionPlayerLocations.add(location);
					}
				}

				// Add up scores for all players in region
				for (Player player : Raid.playersInRegion) {
					if (Raid.raidKills.containsKey(player.getName())) {
						scoreCounter += Raid.raidKills.get(player.getName());
					}
				}

				spawnMobs(rand, regionPlayerLocations, scoreCounter, mMMobNames, chances, priorities, maxMobsPerPlayer, mobLevel);

			}
		}, 0L, 20L / conversionSpawnRateMultiplier);
	}

	// getMobsFromConfig Method
	public static void getMobsFromConfig() {
		Set<String> mmMobs = getConfigSection("RaidMobs").getKeys(false); // only
																												// gets
		// top keys
		// converts set to arraylist
		Raid.mMMobNames.addAll(mmMobs);
		// gets chance and priority data for each mob name
		for (String name : Raid.mMMobNames) {
			double chance = getConfigSection("RaidMobs").getDouble(name + ".Chance");
			int priority = getConfigSection("RaidMobs").getInt(name + ".Priority");
			Raid.chances.add(chance);
			Raid.priorities.add(priority);
		}
	}

	// isCancelledRaid Method
	public static boolean isCancelledRaid(String tier, CommandSender sender) {
		if (Main.cancelledRaid) {
			String raidCancelledTitle = getConfigString("RaidCancelledTitle");
			String raidCancelledSubtitle = getConfigString("RaidCancelledSubtitle");
			if (raidCancelledTitle.contains("@TIER")) {
				raidCancelledTitle = raidCancelledTitle.replaceAll("@TIER", tier);
			}
			if (raidCancelledSubtitle.contains("@TIER")) {
				raidCancelledSubtitle = raidCancelledSubtitle.replaceAll("@TIER", tier);
			}
			if (Raid.region != null) {
				if (raidCancelledTitle.contains("@REGION")) {
					raidCancelledTitle = raidCancelledTitle.replaceAll("@REGION", Raid.region.getId());
				}
				if (raidCancelledSubtitle.contains("@REGION")) {
					raidCancelledSubtitle = raidCancelledSubtitle.replaceAll("@REGION", Raid.region.getId());
				}
			}
			if (raidCancelledTitle.contains("@SENDER")) {
				raidCancelledTitle = raidCancelledTitle.replaceAll("@SENDER", sender.getName());
			}
			if (raidCancelledSubtitle.contains("@SENDER")) {
				raidCancelledSubtitle = raidCancelledSubtitle.replaceAll("@SENDER", sender.getName());
			}
			for (Player player : Raid.playersInRegion) {
				player.sendTitle(color(raidCancelledTitle), color(raidCancelledSubtitle), 10, 60, 10);
			}
			for (AbstractEntity entity : Raid.MmEntityList) {
				if (entity.isLiving()) {
					entity.remove();
				}
			}
			return true;
		} else {
			return false;
		}
	}

	// isWonRaid Method
	public static boolean isWonRaid(String tier, int goal, String boss, int mobLevel, CommandSender sender) {
		if (Raid.totalKills >= goal) {
			if (boss.equalsIgnoreCase("NONE")) {
				String raidWinTitle = getConfigString("RaidWinTitle");
				String raidWinSubtitle = getConfigString("RaidWinSubtitle");
				if (raidWinTitle.contains("@TIER")) {
					raidWinTitle = raidWinTitle.replaceAll("@TIER", tier);
				}
				if (raidWinSubtitle.contains("@TIER")) {
					raidWinSubtitle = raidWinSubtitle.replaceAll("@TIER", tier);
				}
				if (Raid.region != null) {
					if (raidWinTitle.contains("@REGION")) {
						raidWinTitle = raidWinTitle.replaceAll("@REGION", Raid.region.getId());
					}
					if (raidWinSubtitle.contains("@REGION")) {
						raidWinSubtitle = raidWinSubtitle.replaceAll("@REGION", Raid.region.getId());
					}
				}
				if (raidWinTitle.contains("@SENDER")) {
					raidWinTitle = raidWinTitle.replaceAll("@SENDER", sender.getName());
				}
				if (raidWinSubtitle.contains("@SENDER")) {
					raidWinSubtitle = raidWinSubtitle.replaceAll("@SENDER", sender.getName());
				}
				for (Player player : Raid.playersInRegion) {
					player.sendTitle(color(raidWinTitle), color(raidWinSubtitle), 10, 60, 10);
				}

				for (AbstractEntity entity : Raid.MmEntityList) {
					if (entity.isLiving()) {
						entity.damage(1000);
					}
				}

				if (Main.getInstance().getConfig().getBoolean("UseWinLossCommands")) {
					try {
						List<String> globalCommands = getConfigStringList("RaidWinCommands.Global");
						for (String command : globalCommands) {
							if (Raid.region != null) {
								if (command.contains("@REGION")) {
									command = command.replaceAll("@REGION", Raid.region.getId());
								}
							}
							if (command.contains("@TIER")) {
								command = command.replaceAll("@TIER", tier);
							}
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}

					try {
						List<String> perPlayerCommands = getConfigStringList("RaidWinCommands.PerPlayer");
						for (String command : perPlayerCommands) {
							if (Raid.region != null) {
								if (command.contains("@REGION")) {
									command = command.replaceAll("@REGION", Raid.region.getId());
								}
							}
							if (command.contains("@TIER")) {
								command = command.replaceAll("@TIER", tier);
							}
							for (String key : Raid.raidKills.keySet()) {
								String playerCommand = command;
								if (command.contains("@PLAYER")) {
									playerCommand = playerCommand.replaceAll("@PLAYER", key);
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand);
								}
							}
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
				return true;
			} else if (!Raid.bossSpawned) {
				for (Player player : Raid.playersInRegion) {
					player.sendTitle(color("&4&lBoss Spawned!"), color("&6Kill the boss to win the raid"), 10, 60, 10);
				}
				Raid.bossSpawned = true;
				Random rand = new Random();
				int randIdx = rand.nextInt(Raid.playersInRegion.size()); // pick a random idx from playerlist
				int x = Raid.playersInRegion.get(randIdx).getLocation().getBlockX();
				int y = Raid.playersInRegion.get(randIdx).getLocation().getBlockY();
				int z = Raid.playersInRegion.get(randIdx).getLocation().getBlockZ();
				World w = Raid.playersInRegion.get(randIdx).getWorld();
				Location spawnLocation = new Location(w, x, y, z);

				ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(boss, spawnLocation, mobLevel);
				try {
					AbstractEntity entityOfMob = mob.getEntity();
					Raid.bossEntity = entityOfMob;
					Raid.MmEntityList.add(entityOfMob);
					Raid.mobsSpawned++;
					Raid.mobsLeft++;
				} catch (NullPointerException ignored) {}
				return false;
			} else {
				return false;
			}

		}
		return false;
	}

	// isLostRaid Method
	public static boolean isLostRaid(String tier, int goal, int minutes, CommandSender sender) {
		if (Raid.countdown == 0 && minutes == 0) {
			String raidLoseTitle = getConfigString("RaidLoseTitle");
			String raidLoseSubtitle = getConfigString("RaidLoseSubtitle");
			if (raidLoseTitle.contains("@TIER")) {
				raidLoseTitle = raidLoseTitle.replaceAll("@TIER", tier);
			}
			if (raidLoseSubtitle.contains("@TIER")) {
				raidLoseSubtitle = raidLoseSubtitle.replaceAll("@TIER", tier);
			}
			if (Raid.region != null) {
				if (raidLoseTitle.contains("@REGION")) {
					raidLoseTitle = raidLoseTitle.replaceAll("@REGION", Raid.region.getId());
				}
				if (raidLoseSubtitle.contains("@REGION")) {
					raidLoseSubtitle = raidLoseSubtitle.replaceAll("@REGION", Raid.region.getId());
				}
			}
			if (raidLoseTitle.contains("@SENDER")) {
				raidLoseTitle = raidLoseTitle.replaceAll("@SENDER", sender.getName());
			}
			if (raidLoseSubtitle.contains("@SENDER")) {
				raidLoseSubtitle = raidLoseSubtitle.replaceAll("@SENDER", sender.getName());
			}
			if (Raid.totalKills < goal) {
				// raid lost

				for (Player player : Raid.playersInRegion) {
					player.sendTitle(color(raidLoseTitle), color(raidLoseSubtitle), 10, 60, 10);
				}

				if (!getConfigBoolean("MobsStayOnRaidLoss")) {
					for (AbstractEntity entity : Raid.MmEntityList) {
						if (entity.isLiving()) {
							entity.remove();
						}
					}
				}

				if (getConfigBoolean("UseWinLossCommands")) {
					try {
						List<String> globalCommands = getConfigStringList("RaidLoseCommands.Global");
						for (String command : globalCommands) {
							if (Raid.region != null) {
								if (command.contains("@REGION")) {
									command = command.replaceAll("@REGION", Raid.region.getId());
								}
							}
							if (command.contains("@TIER")) {
								command = command.replaceAll("@TIER", tier);
							}
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}

					try {
						List<String> perPlayerCommands = getConfigStringList("RaidLoseCommands.PerPlayer");
						for (String command : perPlayerCommands) {
							if (Raid.region != null) {
								if (command.contains("@REGION")) {
									command = command.replaceAll("@REGION", Raid.region.getId());
								}
							}
							if (command.contains("@TIER")) {
								command = command.replaceAll("@TIER", tier);
							}
							for (String key : Raid.raidKills.keySet()) {
								String playerCommand = command;
								if (command.contains("@PLAYER")) {
									playerCommand = playerCommand.replaceAll("@PLAYER", key);
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand);
								}
							}
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}

			}

			return true;
		} else {
			return false;
		}
	}

}
