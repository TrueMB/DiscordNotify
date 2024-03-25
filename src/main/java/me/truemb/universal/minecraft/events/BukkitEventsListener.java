package me.truemb.universal.minecraft.events;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpleyaml.configuration.file.YamlConfiguration;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.BukkitPlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class BukkitEventsListener implements Listener {
	
	private DiscordNotifyMain plugin;
	private BukkitAudiences adventure;
	
	public BukkitEventsListener(DiscordNotifyMain plugin, BukkitAudiences adventure) {
		this.plugin = plugin;
		this.adventure = adventure;
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
		
		boolean b = this.plugin.getListener().onPlayerMessage(up, message);
		if(b)
			e.setCancelled(true);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {

		Player p = e.getPlayer();
		
		UniversalPlayer up = new BukkitPlayer(p, this.adventure);

		this.plugin.getUniversalServer().addPlayer(up);
		this.plugin.getListener().onPlayerJoin(up, null);
		
	}
	
	@EventHandler
	public void onKick(PlayerKickEvent e) {
		//
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {

		Player p = e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null)
			return;

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
	
	@EventHandler
	public void onAdvancement(PlayerAdvancementDoneEvent e) {
		
		Player p = e.getPlayer();
		
		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		
		NamespacedKey key = e.getAdvancement().getKey();
		if(key.getNamespace() != NamespacedKey.MINECRAFT)
			return;

        InputStream configInputStream = getClass().getClassLoader().getResourceAsStream("advancements.yml");
        
		try {
	        @SuppressWarnings("deprecation")
	        YamlConfiguration conf = YamlConfiguration.loadConfiguration(configInputStream);
			String advancement = conf.getString(key.getKey().toString());
			
			if(advancement == null)
				return;
			
			this.plugin.getListener().onPlayerAdvancement(up, advancement);
			
		} catch (IOException ex) {} //Couldn't find the advancement
        
	}
	
}
