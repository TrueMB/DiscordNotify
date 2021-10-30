package me.truemb.disnotify.spigot.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;

public class MC_ChatListener implements Listener{

	private DiscordManager discordManager;
	private ConfigCacheHandler configCache;
	
	private HashMap<UUID, Boolean> discordChatEnabled;

	public MC_ChatListener(DiscordManager discordManager, ConfigCacheHandler configCache, HashMap<UUID, Boolean> discordChatEnabled) {
		this.discordManager = discordManager;
		this.configCache = configCache;
		
		this.discordChatEnabled = discordChatEnabled;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		if(e.isCancelled())
			return;

		//Check if extra Chat is enabled for ChatSyncing
		if(this.configCache.getOptionBoolean("Chat.enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD CHAT MESSAGE
		long channelId = this.configCache.getChannelId(FeatureType.Chat);
			
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", e.getMessage());
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", "");
			
		if(this.configCache.useEmbedMessage(FeatureType.Chat)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
}
