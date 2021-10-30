package me.truemb.disnotify.bungeecord.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BC_ChatListener implements Listener{

	private DiscordManager discordManager;
	private ConfigCacheHandler configCache;
	
	private HashMap<UUID, Boolean> discordChatEnabled;

	public BC_ChatListener(DiscordManager discordManager, ConfigCacheHandler configCache, HashMap<UUID, Boolean> discordChatEnabled) {
		this.configCache = configCache;
		this.discordManager = discordManager;
		
		this.discordChatEnabled = discordChatEnabled;
	}
	
	@EventHandler
	public void onChat(ChatEvent e) {
		
		if(!(e.getSender() instanceof ProxiedPlayer))
			return;
		
		ProxiedPlayer p = (ProxiedPlayer) e.getSender();
		UUID uuid = p.getUniqueId();
		
		if(e.isCancelled())
			return;
		
		if(e.isCommand())
			return;
		
		//Check if extra Chat is enabled for ChatSyncing
		if(this.configCache.getOptionBoolean("Chat.enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD DEATH MESSAGE
		long channelId = this.configCache.getChannelId(FeatureType.Chat);
			
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", e.getMessage());
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", p.getServer().getInfo().getName());
		
		if(this.configCache.useEmbedMessage(FeatureType.Chat)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
}
