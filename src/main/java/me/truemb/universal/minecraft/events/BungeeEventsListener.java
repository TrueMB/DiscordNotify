package me.truemb.universal.minecraft.events;

import java.util.UUID;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.BungeePlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeEventsListener implements Listener {
	
	private DiscordNotifyMain plugin;
	private BungeeAudiences adventure;
	
	public BungeeEventsListener(DiscordNotifyMain plugin, BungeeAudiences adventure) {
		this.plugin = plugin;
		this.adventure = adventure;
	}

	@EventHandler
	public void onChat(ChatEvent e) {
		
		if(e.isCancelled())
			return;
		
		if(e.isCommand())
			return;
		
		if(!(e.getSender() instanceof ProxiedPlayer))
			return;
		
		ProxiedPlayer p = (ProxiedPlayer) e.getSender();
		UUID uuid = p.getUniqueId();

		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = e.getMessage();
		
		boolean b = this.plugin.getListener().onPlayerMessage(up, message);
		if(b)
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onConnect(ServerConnectedEvent e) {

		ProxiedPlayer p = (ProxiedPlayer) e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		String newServerName = e.getServer().getInfo().getName();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null)
			this.plugin.getUniversalServer().addPlayer(up = new BungeePlayer(p, this.adventure));
		
		//PLAYER JOINS PROXY - Server current null
		String oldServerName = up.getServer() != null ? String.valueOf(up.getServer()) : null; //Clone the string, to not override the oldServer
		up.setServer(newServerName);
		
		if(oldServerName == null)
			this.plugin.getListener().onPlayerJoin(up, newServerName); //JOINING PROXY
		else
			this.plugin.getListener().onPlayerServerChange(up, oldServerName, newServerName); //CHANGING SERVER
		
	}

	@EventHandler
	public void onKick(ServerKickEvent e) {
		//
	}
	
	//LEAVING PROXY
	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent e) {

		ProxiedPlayer p = (ProxiedPlayer) e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null)
			return;
		
		String serverName = up.getServer();

		this.plugin.getUniversalServer().removePlayer(up);
		this.plugin.getListener().onPlayerQuit(up, serverName);
		
	}
	
}
