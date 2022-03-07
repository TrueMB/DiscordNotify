package me.truemb.discordnotify.commands;

import java.util.UUID;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;

public class DN_DChatCommand {
	
	private DiscordNotifyMain instance;
	
	public DN_DChatCommand(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}
	
	public void onCommand(UniversalPlayer up, String[] args) {

		UUID uuid = up.getUUID();
		
		if(!this.instance.getDiscordChatEnabled().containsKey(uuid) || !this.instance.getDiscordChatEnabled().get(uuid)) {
			
			this.instance.getDiscordChatEnabled().put(uuid, true);
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("discordChatEnable", true));
			return;
			
		}else {
			
			this.instance.getDiscordChatEnabled().put(uuid, false);
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("discordChatDisable", true));
			return;
			
		}
	}
	
}
