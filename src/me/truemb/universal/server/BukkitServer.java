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
		File paperYml = new File("paper.yml");
		YamlConfiguration paperCfg = YamlConfiguration.loadConfiguration(paperYml);
		
		org.bukkit.configuration.file.YamlConfiguration spigotCfg = Bukkit.spigot().getConfig();
		return paperYml.exists() ? spigotCfg.getBoolean("settings.bungeecord") || paperCfg.getBoolean("settings.velocity-support.enabled") : spigotCfg.getBoolean("settings.bungeecord");
	}


}
