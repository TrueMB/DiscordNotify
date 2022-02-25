package _me.truemb.universal.server;

import java.util.logging.Logger;

import org.bukkit.Bukkit;

public class BukkitServer extends UniversalServer {
	
	@Override
	public BukkitServer getBukkitServer() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return Bukkit.getLogger();
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
		org.bukkit.configuration.file.YamlConfiguration spigotCfg = Bukkit.spigot().getConfig();
		return spigotCfg.getBoolean("settings.bungeecord");
	}


}
