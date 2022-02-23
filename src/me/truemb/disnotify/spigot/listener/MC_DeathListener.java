package me.truemb.disnotify.spigot.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.listener.DiscordNotifyListener;
import me.truemb.disnotify.spigot.main.Main;

public class MC_DeathListener implements Listener{

	private Main instance;
	private DiscordNotifyListener listener;

	public MC_DeathListener(Main plugin, DiscordNotifyListener listener) {
		this.instance = plugin;
		this.listener = listener;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		UUID uuid = p.getUniqueId();
		
		if(p.hasMetadata("NPC")) //IS NPC?
			return;
		
		String deathMessage = e.getDeathMessage();

		//IF BUNGEECORD SUB SERVER, THEN STOP HERE. IF ONLY SPIGOT, THEN SEND MESSAGES
		if(this.instance.getPluginInformations().isBungeeCordSubServer()) {
			//NOTIFY THE BUNGEECORD WITH THE EVENT DEATH
			this.instance.getMessagingManager().sendPlayerDeath(p, deathMessage);
			return;
		}
		
		this.listener.onPlayerDeath(new UniversalPlayer(uuid, p.getName()), deathMessage);
	}
}
