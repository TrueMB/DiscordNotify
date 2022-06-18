package me.truemb.universal.server;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.simpleyaml.configuration.file.YamlConfiguration;

public class BukkitServer extends UniversalServer {
		
	@Override
	public BukkitServer getBukkitServer() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return Logger.getLogger("DiscordNotify");
	}

	@Override
	public void broadcast(String message) {
		Bukkit.broadcastMessage(message);
	}

	@Override
	public void broadcast(String message, String permission) {
		Bukkit.broadcast(message, permission);
	}

	@Override
	public boolean isOnlineMode() {
		return Bukkit.getOnlineMode();
	}

	@Override
	public boolean isProxySubServer() {
				
		File oldPaperYml = new File("paper.yml");
		File newPaperYml = new File("config", "paper-global.yml");
		
		//VELOCITY BUNGEE SERVER?
		if(oldPaperYml.exists()) {
			YamlConfiguration oldPaperCfg = YamlConfiguration.loadConfiguration(oldPaperYml);
			boolean paperBungee = oldPaperCfg.getBoolean("settings.velocity-support.enabled");
			if(paperBungee)
				return true;
		}else if(newPaperYml.exists()) {
			YamlConfiguration newPaperCfg = YamlConfiguration.loadConfiguration(oldPaperYml);
			boolean paperBungee = newPaperCfg.getBoolean("proxies.velocity.enabled");
			if(paperBungee)
				return true;
		}
		
		//FALLBACK, IF NO PAPER SERVER
		return Bukkit.spigot().getConfig().getBoolean("settings.bungeecord");
	}
}
