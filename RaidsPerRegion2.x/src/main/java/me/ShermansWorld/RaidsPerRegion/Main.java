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
		this.saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new MobListener(), this);
		hook = new PAPIHook();
		hook.register();
		//this.getConfig().options().copyDefaults(false);
		
		//initialize commands
		getCommand("raidsperregion").setExecutor(new RaidsPerRegionCommands(this));
		getCommand("raid").setExecutor(new RaidCommands(this));
	}

	@Override
	public void onDisable() {
		hook.unregister();
		HandlerList.unregisterAll(this);
		getCommand("raidsperregion").setExecutor(null);
		getCommand("raid").setExecutor(null);
		getServer().getScheduler().cancelTasks(this);
		super.onDisable();
	}

	public static Main getInstance() {
		return instance;
	}
	
}
