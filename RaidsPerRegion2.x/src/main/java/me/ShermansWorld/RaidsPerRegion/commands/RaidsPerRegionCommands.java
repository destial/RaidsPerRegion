package me.ShermansWorld.RaidsPerRegion.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.ShermansWorld.RaidsPerRegion.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RaidsPerRegionCommands implements CommandExecutor, TabExecutor {
	
	private final Main plugin;

	public RaidsPerRegionCommands(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("raidsperregion.reload")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to do this");
			return false;
		}

		if (args.length != 0) {
			if (args[0].equalsIgnoreCase("source")) {
				sender.sendMessage("[RaidsPerRegion] This plugin is an open source project developed by ShermansWorld and KristOJa");
				sender.sendMessage("[RaidsPerRegion] Link to source code: https://github.com/ShermansWorld/RaidsPerRegion/");
				return false;
			} else if (args[0].equalsIgnoreCase("version")) {
				sender.sendMessage("[RaidsPerRegion] Your server is running RaidsPerRegion Version 2.2 for Minecraft 1.18.2");
				return false;
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("raidsperregion.reload")) {
					sender.sendMessage(ChatColor.RED + "You do not have permission to do this");
					return false;
				}
				plugin.reloadConfig();
				plugin.saveDefaultConfig();
				sender.sendMessage("[RaidsPerRegion] config.yml reloaded");
				return false;
			}
		}
		sender.sendMessage("[RaidsPerRegion] Invalid arguments");
		sender.sendMessage("[RaidsPerRegion] Reload Config: /raidsperregion reload");
		sender.sendMessage("[RaidsPerRegion] View Source Code: /raidsperregion source");
		return false;
		
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1 && sender.hasPermission("raidsperregion.reload")) {
			completions.add("source");
			completions.add("version");
			completions.add("reload");

			return completions;
		}else if(args.length == 1) {
			completions.add("source");
			completions.add("version");
		}

		return Collections.emptyList();
	}
	
}
