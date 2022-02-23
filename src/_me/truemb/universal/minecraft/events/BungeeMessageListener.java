package _me.truemb.universal.minecraft.events;

import java.util.UUID;

import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeMessageListener implements Listener {
	
	private DiscordNotifyMain plugin;
	
	public BungeeMessageListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onChat(ChatEvent e) {
		
		if(!(e.getSender() instanceof ProxiedPlayer))
			return;
		
		ProxiedPlayer p = (ProxiedPlayer) e.getSender();
		
		UUID uuid = p.getUniqueId();
		String name = p.getName();
		
		UniversalPlayer up = new UniversalPlayer(uuid, name);
		String message = e.getMessage();
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}
}
