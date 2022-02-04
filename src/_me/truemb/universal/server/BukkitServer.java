package _me.truemb.universal.server;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import eu.mcdb.universal.player.UniversalPlayer;

public class BukkitServer implements ServerInterface{

	@Override
	public Collection<UniversalPlayer> getOnlinePlayers() {
		return null; //TODO CACHING
	}

	@Override
	public UniversalPlayer getPlayer(UUID uuid) {
		return null;
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


}
