package me.ShermansWorld.RaidsPerRegion;

import me.ShermansWorld.RaidsPerRegion.papi.PAPIHook;
import org.bukkit.plugin.java.JavaPlugin;

import me.ShermansWorld.RaidsPerRegion.commands.RaidCommands;
import me.ShermansWorld.RaidsPerRegion.commands.RaidsPerRegionCommands;
import me.ShermansWorld.RaidsPerRegion.listeners.MobListener;
import me.ShermansWorld.RaidsPerRegion.tabCompletion.RaidTabCompletion;
import me.ShermansWorld.RaidsPerRegion.tabCompletion.RaidsPerRegionTabCompletion;

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
		new RaidsPerRegionCommands(this);
		this.getCommand("raidsperregion").setTabCompleter(new RaidsPerRegionTabCompletion());//Tab completer for raidspreregion command
		new RaidCommands(this);
		this.getCommand("raid").setTabCompleter(new RaidTabCompletion());//Tab completer for raid command
	}

	@Override
	public void onDisable() {
		hook.unregister();
		super.onDisable();
	}

	public static Main getInstance() {
		return instance;
	}
	
}
