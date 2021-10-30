package me.truemb.disnotify.spigot.commands;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;

public class MC_StaffCommand extends BukkitCommand{

	private ConfigCacheHandler configCache;
	private DiscordManager discordManager;
    public HashMap<UUID, Boolean> staffChatDisabled;
	
	public MC_StaffCommand(DiscordManager discordManager, ConfigCacheHandler configCache, HashMap<UUID, Boolean> staffHash) {
		super("staff");
		
		this.configCache = configCache;
		this.discordManager = discordManager;
		this.staffChatDisabled = staffHash;
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.configCache.getMinecraftMessage("console", false));
			return true;
		}
		
		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.configCache.isFeatureEnabled(FeatureType.Staff)) {
			p.sendMessage(this.configCache.getMinecraftMessage("disabledFeature", true));
			return true;
		}

		if(!p.hasPermission(this.configCache.getPermission("StaffChat"))) {
			p.sendMessage(this.configCache.getMinecraftMessage("perm", false));
			return true;
		}
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.staffChatDisabled.put(uuid, false);
				p.sendMessage(this.configCache.getMinecraftMessage("staffEnable", true));
				return true;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.staffChatDisabled.put(uuid, true);
				p.sendMessage(this.configCache.getMinecraftMessage("staffDisable", true));
				return true;
			}
		}else if(args.length < 1) {
			p.sendMessage(this.configCache.getMinecraftMessage("staffHelp", true));
			return true;
		}
		
		String message = "";
		for(int i = 0; i < args.length; i++) {
			message += args[i] + " ";
		}
		message = message.substring(0, message.length() - 1);
		
		//ALL PLAYERS INGAME
		for(Player all : Bukkit.getOnlinePlayers()) {
			UUID uuidAll = all.getUniqueId();
			if(all.hasPermission(this.configCache.getPermission("StaffChat"))) {
				if(p.equals(all) || !this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
					all.sendMessage(this.configCache.getMinecraftMessage("minecraftStaffMessage", true).replace("%Message%", message));
				}
			}
		}
		
		//DISCORD STAFF MESSAGE
		long channelId = this.configCache.getChannelId(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		if(this.configCache.useEmbedMessage(FeatureType.Staff)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "StaffEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "StaffMessage", placeholder);
		}
		
		return true;
	}

}
