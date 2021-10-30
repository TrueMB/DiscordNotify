package me.truemb.disnotify.bungeecord.commands;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.utils.ConfigCacheHandler;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BC_DChatCommand extends Command{

	private ConfigCacheHandler configCache;
	private HashMap<UUID, Boolean> discordChatEnabled;
	
	public BC_DChatCommand(ConfigCacheHandler configCache, HashMap<UUID, Boolean> discordChatEnabled) {
		super("dchat");
		this.configCache = configCache;
		this.discordChatEnabled = discordChatEnabled;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("console", false)));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid)) {
			
			this.discordChatEnabled.put(uuid, true);
			p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("discordChatEnable", true)));
			return;
			
		}else {
			
			this.discordChatEnabled.put(uuid, false);
			p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("discordChatDisable", true)));
			return;
			
		}

	}
}