package _me.truemb.universal.minecraft.events;

import java.util.UUID;

import _me.truemb.universal.player.BungeePlayer;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeEventsListener implements Listener {
	
	private DiscordNotifyMain plugin;
	
	public BungeeEventsListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
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
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	@EventHandler
	public void onConnect(ServerConnectEvent e) {

		if(e.getReason() == Reason.UNKNOWN) 
			return;

		ProxiedPlayer p = (ProxiedPlayer) e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		String serverName = e.getTarget().getName();

		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null)
			this.plugin.getUniversalServer().addPlayer(up = new BungeePlayer(p));
		
		up.setServer(serverName);
		
		this.plugin.getListener().onPlayerJoin(up, serverName);
	}
	
	@EventHandler
	public void onDisconnect(ServerDisconnectEvent e) {

		ProxiedPlayer p = (ProxiedPlayer) e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		String serverName = e.getTarget().getName();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		this.plugin.getUniversalServer().removePlayer(up);
		
		this.plugin.getListener().onPlayerQuit(up, serverName);
	}
	
}
