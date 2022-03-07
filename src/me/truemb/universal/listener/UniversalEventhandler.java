package me.truemb.universal.listener;

import me.truemb.universal.player.UniversalPlayer;

public abstract class UniversalEventhandler {
	
	/**
	 * Methods doesn't contain NPCs
	 */

	public abstract void onPlayerQuit(UniversalPlayer up, String serverName);
	
	public abstract void onPlayerJoin(UniversalPlayer up, String serverName);
	
	public abstract void onPlayerMessage(UniversalPlayer up, String message);
	
	public abstract void onPlayerDeath(UniversalPlayer up, String message);

}
