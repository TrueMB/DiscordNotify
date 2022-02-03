package me.truemb.disnotify.commands.minecraft;

import java.util.HashMap;
import java.util.UUID;

import eu.mcdb.shaded.universal.Server;
import eu.mcdb.shaded.universal.command.UniversalCommandSender;
import eu.mcdb.shaded.universal.command.api.Command;
import eu.mcdb.shaded.universal.player.UniversalPlayer;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.main.DiscordNotifyPlugin;

public class Staff_Command extends Command{

	private DiscordNotifyPlugin instance;
	
	public Staff_Command(DiscordNotifyPlugin plugin) {
		super("staff", plugin.getConfigManager().getConfig().getString("Permissions.StaffChat"));
		this.instance = plugin;
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void onCommandUsed(UniversalCommandSender sender, String[] args) {

		if(!sender.isPlayer()) {
			sender.sendMessage(this.instance.getConfigManager().getMinecraftMessage("console", false));
			return;
		}
		
		UniversalPlayer p = sender.getPlayer();
		UUID uuid = p.getUniqueId();
		
		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			p.sendMessage(this.instance.getConfigManager().getMinecraftMessage("disabledFeature", true));
			return;
		}

		if(!p.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"))) {
			p.sendMessage(this.instance.getConfigManager().getMinecraftMessage("perm", false));
			return;
		}
		
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("on")) {
				this.instance.staffChatDisabled.put(uuid, false);
				p.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffEnable", true));
				return;
				
			}else if(args[0].equalsIgnoreCase("off")) {
				this.instance.staffChatDisabled.put(uuid, true);
				p.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffDisable", true));
				return;
			}
		}else if(args.length < 1) {
			p.sendMessage(this.instance.getConfigManager().getMinecraftMessage("staffHelp", true));
			return;
		}
		
		String message = "";
		for(int i = 0; i < args.length; i++) {
			message += args[i] + " ";
		}
		message = message.substring(0, message.length() - 1);
		
		//ALL PLAYERS INGAME
		for(UniversalPlayer all : Server.getInstance().getOnlinePlayers()) {
			UUID uuidAll = all.getUniqueId();
			if(all.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"))) {
				if(p.equals(all) || !this.instance.staffChatDisabled.containsKey(uuidAll) || !this.instance.staffChatDisabled.get(uuidAll)) {
					all.sendMessage(this.instance.getConfigManager().getMinecraftMessage("minecraftStaffMessage", true).replaceAll("(?i)%" + "%Message%" + "%", message));
				}
			}
		}
		
		//DISCORD STAFF MESSAGE
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", p.getName());
		placeholder.put("UUID", uuid.toString());
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.Staff)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "StaffEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "StaffMessage", placeholder);
		}
		
		System.out.println("test");
		
		return;
	}

}
