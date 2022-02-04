package _me.truemb.universal.server;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import eu.mcdb.universal.player.UniversalPlayer;

public interface ServerInterface {

	/**
	 * 
	 * @return All OnlinePlayers of the current Server as an UniversalPlayer Object
	 */
	Collection<UniversalPlayer> getOnlinePlayers();
	
	
	/**
	 * 
	 * @param The UUID of the target
	 * @return The UniversalPlayer Object of the Player
	 */
	UniversalPlayer getPlayer(UUID uuid);
	
	/**
	 * 
	 * @return Logger of the current Server Instance
	 */
	Logger getLogger();
	
	/**
	 * 
	 * @param Broadcast a message
	 */
	void broadcast(String message);
	
	/**
	 * 
	 * @param Broadcast a message for player with Permissions
	 */
	void broadcast(String message, String permission);
}
