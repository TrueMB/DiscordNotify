package me.truemb.disnotify.spigot.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.spigot.main.Main;

public class MC_DeathListener implements Listener{

	private Main instance;

	public MC_DeathListener(Main plugin) {
		this.instance = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		UUID uuid = p.getUniqueId();

		if(p.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Death")))
			return;
		
		String deathMessage = e.getDeathMessage();

		//IF BUNGEECORD SUB SERVER, THEN STOP HERE. IF ONLY SPIGOT, THEN SEND MESSAGES
		if(this.instance.getPluginInformations().isBungeeCordSubServer()) {
			//NOTIFY THE BUNGEECORD WITH THE EVENT DEATH
			this.instance.getMessagingManager().sendPlayerDeath(p, deathMessage);
			return;
		}
		
		//DISCORD DEATH MESSAGE
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerDeath);
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", p.getUniqueId().toString());
		placeholder.put("DeathMessage", deathMessage);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerDeath)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "DeathEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerDeathMessage", placeholder);
		}
	}
}
