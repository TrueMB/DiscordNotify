package me.truemb.disnotify.bungeecord.commands;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.manager.ConfigManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BC_DChatCommand extends Command{

	private ConfigManager configManager;
	private HashMap<UUID, Boolean> discordChatEnabled;
	
	public BC_DChatCommand(ConfigManager configManager, HashMap<UUID, Boolean> discordChatEnabled) {
		super("dchat");
		this.configManager = configManager;
		this.discordChatEnabled = discordChatEnabled;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(this.configManager.getMessageAsTextComponent("console", false));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid)) {
			
			this.discordChatEnabled.put(uuid, true);
			p.sendMessage(this.configManager.getMessageAsTextComponent("discordChatEnable", true));
			return;
			
		}else {
			
			this.discordChatEnabled.put(uuid, false);
			p.sendMessage(this.configManager.getMessageAsTextComponent("discordChatDisable", true));
			return;
			
		}

	}
}