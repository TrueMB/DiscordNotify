package me.truemb.disnotify.bungeecord.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class BC_ChatListener implements Listener{

	private DiscordManager discordManager;
	private ConfigManager configManager;
	private PermissionsAPI permsAPI;
	
	private HashMap<UUID, Boolean> discordChatEnabled;

	public BC_ChatListener(DiscordManager discordManager, ConfigManager configManager, PermissionsAPI permsAPI, HashMap<UUID, Boolean> discordChatEnabled) {
		this.configManager = configManager;
		this.discordManager = discordManager;
		this.permsAPI = permsAPI;
		
		this.discordChatEnabled = discordChatEnabled;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(ChatEvent e) {
		
		if(!(e.getSender() instanceof ProxiedPlayer))
			return;
		
		ProxiedPlayer p = (ProxiedPlayer) e.getSender();
		UUID uuid = p.getUniqueId();

		if(this.configManager.getConfig().getBoolean("Options.EnableBypassPermission") && p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Chat")))
			return;
		
		if(e.isCancelled())
			return;
		
		if(e.isCommand())
			return;
		
		//Check if extra Chat is enabled for ChatSyncing
		if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD MESSAGE
		String server = p.getServer().getInfo().getName();
		String group = this.permsAPI.getPrimaryGroup(uuid);
		
		long channelId;
		if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat"))
			channelId = this.configManager.getConfig().getLong("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + server);
		else
			channelId = this.configManager.getChannelID(FeatureType.Chat);
			
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", e.getMessage());
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("group", group == null ? "" : group);
		placeholder.put("server", server);
		
		if(this.configManager.useEmbedMessage(FeatureType.Chat)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
}
