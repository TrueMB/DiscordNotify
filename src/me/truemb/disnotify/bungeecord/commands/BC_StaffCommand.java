package me.truemb.disnotify.bungeecord.commands;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BC_StaffCommand extends Command{

	private ConfigCacheHandler configCache;
	private DiscordManager discordManager;
    public HashMap<UUID, Boolean> staffChatDisabled;
	
	public BC_StaffCommand(DiscordManager discordManager, ConfigCacheHandler configCache, HashMap<UUID, Boolean> staffHash) {
		super("staff");
		this.configCache = configCache;
		this.discordManager = discordManager;
		this.staffChatDisabled = staffHash;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("console", false)));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.configCache.isFeatureEnabled(FeatureType.Staff)) {
			p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("disabledFeature", true)));
			return;
		}

		if(!p.hasPermission(this.configCache.getPermission("StaffChat"))) {
			p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("perm", false)));
			return;
		}
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.staffChatDisabled.put(uuid, false);
				p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("staffEnable", true)));
				return;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.staffChatDisabled.put(uuid, true);
				p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("staffDisable", true)));
				return;
			}
		}else if(args.length < 1) {
			p.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("staffHelp", true)));
			return;
		}
		
		String message = "";
		for(int i = 0; i < args.length; i++) {
			message += args[i] + " ";
		}
		message = message.substring(0, message.length() - 1);
		
		//ALL PLAYERS INGAME
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UUID uuidAll = all.getUniqueId();
			if(all.hasPermission(this.configCache.getPermission("StaffChat"))) {
				if(p.equals(all) || !this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
					all.sendMessage(new TextComponent(this.configCache.getMinecraftMessage("minecraftStaffMessage", true).replace("%Message%", message)));
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
		
		return;
	}
}