package _me.truemb.universal.minecraft.events;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import _me.truemb.universal.player.BukkitPlayer;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class BukkitEventsListener implements Listener {
	
	private DiscordNotifyMain plugin;
	
	public BukkitEventsListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		
		if(e.isCancelled())
			return;
		
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = e.getMessage();

		if(message.startsWith("/"))
			return;
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {

		Player p = e.getPlayer();
		
		UniversalPlayer up = new BukkitPlayer(p);
		this.plugin.getUniversalServer().addPlayer(up);
		
		this.plugin.getListener().onPlayerJoin(up, null);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {

		Player p = e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		this.plugin.getUniversalServer().removePlayer(up);
		
		this.plugin.getListener().onPlayerQuit(up, null);
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		
		Player p = e.getEntity();
		
		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String deathMessage = e.getDeathMessage();
		
		this.plugin.getListener().onPlayerDeath(up, deathMessage);
	}
	
}
