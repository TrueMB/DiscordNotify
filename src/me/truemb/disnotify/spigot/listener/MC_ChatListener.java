package me.truemb.disnotify.spigot.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;

public class MC_ChatListener implements Listener{

	private DiscordManager discordManager;
	private ConfigManager configManager;
	private PermissionsAPI permsAPI;
	
	private HashMap<UUID, Boolean> discordChatEnabled;

	public MC_ChatListener(DiscordManager discordManager, ConfigManager configManager, PermissionsAPI permsAPI, HashMap<UUID, Boolean> discordChatEnabled) {
		this.discordManager = discordManager;
		this.configManager = configManager;
		this.permsAPI = permsAPI;
		
		this.discordChatEnabled = discordChatEnabled;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();

		if(this.configManager.getConfig().getBoolean("Options.EnableBypassPermission") && p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Chat")))
			return;
		
		if(e.isCancelled())
			return;

		//Check if extra Chat is enabled for ChatSyncing
		if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD CHAT MESSAGE
		long channelId = this.configManager.getChannelID(FeatureType.Chat);
		String group = this.permsAPI.getPrimaryGroup(uuid);
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", e.getMessage());
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("group", group == null ? "" : group);
		placeholder.put("server", "");
			
		if(this.configManager.useEmbedMessage(FeatureType.Chat)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
}
