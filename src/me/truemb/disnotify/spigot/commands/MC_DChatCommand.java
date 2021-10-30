package me.truemb.disnotify.spigot.commands;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.disnotify.utils.ConfigCacheHandler;

public class MC_DChatCommand extends BukkitCommand{

	private ConfigCacheHandler configCache;
	private HashMap<UUID, Boolean> discordChatEnabled;
	
	public MC_DChatCommand(ConfigCacheHandler configCache, HashMap<UUID, Boolean> discordChatEnabled) {
		super("dchat");
		this.configCache = configCache;
		this.discordChatEnabled = discordChatEnabled;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.configCache.getMinecraftMessage("console", false));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid)) {
			
			this.discordChatEnabled.put(uuid, true);
			p.sendMessage(this.configCache.getMinecraftMessage("discordChatEnable", true));
			return true;
			
		}else {
			
			this.discordChatEnabled.put(uuid, false);
			p.sendMessage(this.configCache.getMinecraftMessage("discordChatDisable", true));
			return true;
			
		}
		
	}
}