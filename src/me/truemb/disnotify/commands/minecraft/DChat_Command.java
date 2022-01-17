package me.truemb.disnotify.commands.minecraft;

import java.util.UUID;

import eu.mcdb.universal.command.api.Command;
import eu.mcdb.universal.player.UniversalPlayer;
import me.truemb.disnotify.main.DiscordNotifyPlugin;

public class DChat_Command extends Command{

	private DiscordNotifyPlugin instance;
	
	public DChat_Command(DiscordNotifyPlugin plugin) {
		super("dchat");
		this.instance = plugin;
		
		setCommandHandler(sender -> {
			
			if(!sender.isPlayer()) {
				sender.sendMessage(this.instance.getConfigManager().getMinecraftMessage("console", false));
				return true;
			}
			UniversalPlayer player = sender.getPlayer();
			UUID uuid = player.getUniqueId();
			
			if(!this.instance.discordChatEnabled.containsKey(uuid) || !this.instance.discordChatEnabled.get(uuid)) {
				
				this.instance.discordChatEnabled.put(uuid, true);
				player.sendMessage(this.instance.getConfigManager().getMinecraftMessage("discordChatEnable", true));
				return true;
				
			}else {
				
				this.instance.discordChatEnabled.put(uuid, false);
				player.sendMessage(this.instance.getConfigManager().getMinecraftMessage("discordChatDisable", true));
				return true;
				
			}
			
		});
	}

}
