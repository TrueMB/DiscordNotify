package me.truemb.discordnotify.commands;

import java.util.HashMap;
import java.util.UUID;

import club.minnced.discord.webhook.WebhookClient;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.MinotarTypes;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;

public class DN_StaffCommand {
	
	private DiscordNotifyMain instance;
	
	public DN_StaffCommand(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}
	
	public void onCommand(UniversalPlayer up, String[] args) {

		UUID uuid = up.getUUID();
		
		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("disabledFeature", true));
			return;
		}

		if(!up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"))) {
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("perm", false));
			return;
		}

		if(args.length == 0) {
			if(this.instance.getStaffChatToggle().containsKey(uuid) && this.instance.getStaffChatToggle().get(uuid)) {
				this.instance.getStaffChatToggle().put(uuid, false);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffToggleDisable", true));
				return;
				
			}else{
				this.instance.getStaffChatToggle().put(uuid, true);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffToggleEnable", true));
				return;
			}
		}else if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.instance.getStaffChatDisabled().put(uuid, false);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffEnable", true));
				return;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.instance.getStaffChatDisabled().put(uuid, true);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffDisable", true));
				return;
			}
		}
		
		String message = "";
		for(int i = 0; i < args.length; i++) {
			message += args[i] + " ";
		}
		message = message.substring(0, message.length() - 1);
		
		//ALL PLAYERS INGAME
		for(UniversalPlayer all : this.instance.getUniversalServer().getOnlinePlayers()) {
			UUID uuidAll = all.getUUID();
			if(all.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"))) {
				if(up.equals(all) || !this.instance.getStaffChatDisabled().containsKey(uuidAll) || !this.instance.getStaffChatDisabled().get(uuidAll)) {
					all.sendMessage(this.instance.getConfigManager().getMinecraftMessage("minecraftStaffMessage", true)
							.replaceAll("(?i)%" + "message" + "%", message)
							.replaceAll("(?i)%" + "player" + "%", up.getIngameName())
							.replaceAll("(?i)%" + "server" + "%", up.getServer() != null ? up.getServer() : ""));
				}
			}
		}
		
		//DISCORD STAFF MESSAGE
		String channelId = this.instance.getConfigManager().getChannel(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("Message", message);
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", up.getServer() != null ? up.getServer() : "");
		
		switch (this.instance.getConfigManager().getMessageType(FeatureType.Staff)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "StaffMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Staff.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "StaffEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Staff.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {
				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.Staff, channelId);
				String minotarTypeS = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Staff.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Staff.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
		return;
	}
	
}
