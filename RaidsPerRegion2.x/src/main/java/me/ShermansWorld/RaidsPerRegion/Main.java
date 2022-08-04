package me.ShermansWorld.RaidsPerRegion;

import me.ShermansWorld.RaidsPerRegion.papi.PAPIHook;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import me.ShermansWorld.RaidsPerRegion.commands.RaidCommands;
import me.ShermansWorld.RaidsPerRegion.commands.RaidsPerRegionCommands;
import me.ShermansWorld.RaidsPerRegion.listeners.MobListener;

public class Main extends JavaPlugin {
	
	public static Main instance = null;
	public static boolean cancelledRaid = false;
	public PAPIHook hook;
	
	@Override
	public void onEnable() { //What runs when you start server
		instance = this;
		saveDefaultConfig();

		getServer().getPluginManager().registerEvents(new MobListener(), this);
		getCommand("raidsperregion").setExecutor(new RaidsPerRegionCommands(this));
		getCommand("raid").setExecutor(new RaidCommands(this));

		hook = new PAPIHook();
		hook.register();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		hook.unregister();

		getServer().getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(this);

		getCommand("raidsperregion").setExecutor(null);
		getCommand("raid").setExecutor(null);
		super.onDisable();
	}

	public static Main getInstance() {
		return instance;
	}
	
}
