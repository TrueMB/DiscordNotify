package me.truemb.disnotify.bungeecord.commands;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BC_StaffCommand extends Command{

	private ConfigManager configManager;
	private DiscordManager discordManager;
    public HashMap<UUID, Boolean> staffChatDisabled;
	
	public BC_StaffCommand(DiscordManager discordManager, ConfigManager configManager, HashMap<UUID, Boolean> staffHash) {
		super("staff");
		this.configManager = configManager;
		this.discordManager = discordManager;
		this.staffChatDisabled = staffHash;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(this.configManager.getMessageAsTextComponent("console", false));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		if(!this.configManager.isFeatureEnabled(FeatureType.Staff)) {
			p.sendMessage(this.configManager.getMessageAsTextComponent("disabledFeature", true));
			return;
		}

		if(!p.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
			p.sendMessage(this.configManager.getMessageAsTextComponent("perm", false));
			return;
		}
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.staffChatDisabled.put(uuid, false);
				p.sendMessage(this.configManager.getMessageAsTextComponent("staffEnable", true));
				return;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.staffChatDisabled.put(uuid, true);
				p.sendMessage(this.configManager.getMessageAsTextComponent("staffDisable", true));
				return;
			}
		}else if(args.length < 1) {
			p.sendMessage(this.configManager.getMessageAsTextComponent("staffHelp", true));
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
			if(all.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
				if(p.equals(all) || !this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
					all.sendMessage(new TextComponent(this.configManager.getMinecraftMessage("minecraftStaffMessage", true).replaceAll("(?i)%" + "%Message%" + "%", message)));
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
		
		return;
	}
}