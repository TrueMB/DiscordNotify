package me.truemb.disnotify.spigot.commands;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;

public class MC_StaffCommand extends BukkitCommand{

	private ConfigManager configManager;
	private DiscordManager discordManager;
    public HashMap<UUID, Boolean> staffChatDisabled;
	
	public MC_StaffCommand(DiscordManager discordManager, ConfigManager configManager, HashMap<UUID, Boolean> staffHash) {
		super("staff");
		
		this.configManager = configManager;
		this.discordManager = discordManager;
		this.staffChatDisabled = staffHash;
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.configManager.getMinecraftMessage("console", false));
			return true;
		}
		
		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.configManager.isFeatureEnabled(FeatureType.Staff)) {
			p.sendMessage(this.configManager.getMinecraftMessage("disabledFeature", true));
			return true;
		}

		if(!p.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
			p.sendMessage(this.configManager.getMinecraftMessage("perm", false));
			return true;
		}
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.staffChatDisabled.put(uuid, false);
				p.sendMessage(this.configManager.getMinecraftMessage("staffEnable", true));
				return true;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.staffChatDisabled.put(uuid, true);
				p.sendMessage(this.configManager.getMinecraftMessage("staffDisable", true));
				return true;
			}
		}else if(args.length < 1) {
			p.sendMessage(this.configManager.getMinecraftMessage("staffHelp", true));
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
			if(all.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
				if(p.equals(all) || !this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
					all.sendMessage(this.configManager.getMinecraftMessage("minecraftStaffMessage", true).replaceAll("(?i)%" + "%Message%" + "%", message));
				}
			}
		}
		
		//DISCORD STAFF MESSAGE
		long channelId = this.configManager.getChannelID(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		if(this.configManager.useEmbedMessage(FeatureType.Staff)) {
			this.discordManager.sendEmbedMessage(channelId, uuid, "StaffEmbed", placeholder);
		}else {
			this.discordManager.sendDiscordMessage(channelId, "StaffMessage", placeholder);
		}
		
		return true;
	}

}
