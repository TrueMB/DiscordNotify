package me.truemb.discordnotify.commands;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.discordnotify.enums.FeatureType;
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
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.instance.getStaffChatDisabled().put(uuid, false);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffEnable", true));
				return;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.instance.getStaffChatDisabled().put(uuid, true);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffDisable", true));
				return;
			}
		}else if(args.length < 1) {
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffHelp", true));
			return;
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
					all.sendMessage(this.instance.getConfigManager().getMinecraftMessage("minecraftStaffMessage", true).replaceAll("(?i)%" + "message" + "%", message));
				}
			}
		}
		
		//DISCORD STAFF MESSAGE
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.Staff)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "StaffEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "StaffMessage", placeholder);
		}
		
		return;
	}
	
}
