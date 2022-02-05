package me.truemb.disnotify.bungeecord.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BC_JoinLeaveListener implements Listener{

	private DiscordManager discordManager;
	private ConfigManager configManager;

	public BC_JoinLeaveListener(DiscordManager discordManager, ConfigManager configManager) {
		this.discordManager = discordManager;
		this.configManager = configManager;
	}
	
	@EventHandler
	public void onJoin(ServerConnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		UUID uuid = p.getUniqueId();

		if(this.configManager.getConfig().getBoolean("Options.EnableBypassPermission") && p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Join")))
			return;
		
		//DISCORD JOIN MESSAGE
		if(this.configManager.isFeatureEnabled(FeatureType.PlayerJoinLeave)) {

			if(e.getReason() == Reason.UNKNOWN) 
				return;
			
			String server = e.getTarget().getName();
			long channelId;
			if(this.configManager.getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave"))
				channelId = this.configManager.getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);
			else
				channelId = this.configManager.getChannelID(FeatureType.PlayerJoinLeave);

			HashMap<String, String> placeholder = new HashMap<>();
			placeholder.put("Player", p.getName());
			placeholder.put("UUID", uuid.toString());
			placeholder.put("server", server);
			
			if(this.configManager.useEmbedMessage(FeatureType.PlayerJoinLeave)) {
				this.discordManager.sendEmbedMessage(channelId, uuid, "PlayerJoinEmbed", placeholder);
			}else {
				this.discordManager.sendDiscordMessage(channelId, "PlayerJoinMessage", placeholder);
			}
		}
	}

	@EventHandler
	public void onQuit(ServerDisconnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		UUID uuid = p.getUniqueId();

		if(this.configManager.getConfig().getBoolean("Options.EnableBypassPermission") && p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Leave")))
			return;
		
		//DISCORD LEAVE MESSAGE
		if(this.configManager.isFeatureEnabled(FeatureType.PlayerJoinLeave)) {
			
			String server = e.getTarget().getName();
			long channelId;
			if(this.configManager.getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave"))
				channelId = this.configManager.getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);
			else
				channelId = this.configManager.getChannelID(FeatureType.PlayerJoinLeave);
			
			HashMap<String, String> placeholder = new HashMap<>();
			placeholder.put("Player", p.getName());
			placeholder.put("UUID", uuid.toString());
			placeholder.put("server", server);
			
			if(this.configManager.useEmbedMessage(FeatureType.PlayerJoinLeave)) {
				this.discordManager.sendEmbedMessage(channelId, uuid, "PlayerLeaveEmbed", placeholder);
			}else {
				this.discordManager.sendDiscordMessage(channelId, "PlayerLeaveMessage", placeholder);
			}
		}
	}
}
