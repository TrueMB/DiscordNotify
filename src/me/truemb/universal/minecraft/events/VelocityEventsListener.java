package me.truemb.universal.minecraft.events;

import java.util.UUID;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;
import me.truemb.universal.player.VelocityPlayer;

public class VelocityEventsListener {
	
	//server.getEventManager().register(this, new MyListener());
	//https://velocitypowered.com/wiki/developers/event-api/
	
	private DiscordNotifyMain plugin;
	
	public VelocityEventsListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
	}

	@Subscribe
	public void onChat(PlayerChatEvent e) {
		
		if(e.getResult() == ChatResult.denied())
			return;
		
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();

		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = e.getMessage();
		
		if(message.startsWith("/"))
			return;
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	/**
	 * Triggers if player joins the Proxy or changes the Servers
	 */
	@Subscribe
	public void onServerJoinOrChange(ServerConnectedEvent e) {

		Player p = e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		String newServerName = e.getServer().getServerInfo().getName();
		String oldServerName = e.getPreviousServer().isPresent() ? e.getPreviousServer().get().getServerInfo().getName() : null;
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null) {
			up = new VelocityPlayer(p);
			this.plugin.getUniversalServer().addPlayer(up);
		}

		if(oldServerName != null)
			this.plugin.getListener().onPlayerQuit(up, oldServerName); //OLD SERVER QUIT
		
		up.setServer(newServerName);
		this.plugin.getListener().onPlayerJoin(up, newServerName); //NEW SERVER JOIN
	}
	
	@Subscribe
	public void onDisconnect(DisconnectEvent e) {

		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		String serverName = p.getCurrentServer().get().getServerInfo().getName();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		
		this.plugin.getListener().onPlayerQuit(up, serverName);
		
		this.plugin.getUniversalServer().removePlayer(up);
	}
	
}
