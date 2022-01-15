package me.truemb.disnotify.discord.listener;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.vdurmont.emoji.EmojiParser;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.TextComponent;

public class DC_ChatListener extends ListenerAdapter {
	
	private ConfigManager configManager;
	private PluginInformations pluginInfo;
	
    private HashMap<UUID, Boolean> staffChatDisabled;
    private HashMap<UUID, Boolean> discordChatEnabled;
	
	public DC_ChatListener(ConfigManager configManager, PluginInformations pluginInfo, HashMap<UUID, Boolean> staffHash, HashMap<UUID, Boolean> discordChatEnabled) {
		this.configManager = configManager;
		this.pluginInfo = pluginInfo;
		
		this.staffChatDisabled = staffHash;
		this.discordChatEnabled = discordChatEnabled;
	}

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
    	
    	if(!e.isFromType(ChannelType.TEXT))
    		return;
    	
        long channelId = e.getChannel().getIdLong();
        TextChannel channel = e.getTextChannel();
        String channelName =  EmojiParser.removeAllEmojis(channel.getName()).replace("[^a-zA-Z0-9 -]", "");

	    String message = e.getMessage().getContentDisplay();
	    
	    if(e.getAuthor().isBot())
	    	return;
	    	   	    
	    //CORRECT CHANNEL
	    if(!this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat") && this.configManager.getChannelID(FeatureType.Chat) == channelId) {
	    	
			if(!this.configManager.isFeatureEnabled(FeatureType.Chat))
				return;
		    
			List<String> bypassList = this.configManager.getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }
			
		    final String mcMessage = EmojiParser.parseToAliases(this.configManager.getMinecraftMessage("discordChatMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));

		  
		    if(this.pluginInfo.isBungeeCord()) {

				if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
				    net.md_5.bungee.api.ProxyServer.getInstance().getPlayers().forEach(all -> {
						UUID uuidAll = all.getUniqueId();
						if(this.discordChatEnabled.containsKey(uuidAll) && this.discordChatEnabled.get(uuidAll)) {
							all.sendMessage(new TextComponent(mcMessage));
						}
					});
				}else {
					net.md_5.bungee.api.ProxyServer.getInstance().broadcast(new TextComponent(mcMessage));
				}
		    }else if(!this.pluginInfo.isBungeeCordSubServer()) {
		    	
		    	if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
					org.bukkit.Bukkit.getOnlinePlayers().forEach(all -> {
						UUID uuidAll = all.getUniqueId();
						if(this.discordChatEnabled.containsKey(uuidAll) && this.discordChatEnabled.get(uuidAll)) {
							all.sendMessage(mcMessage);
						}
					});
				}else {
			    	org.bukkit.Bukkit.broadcastMessage(mcMessage);
				}
		    }
	    	
	   	}else if(this.configManager.isFeatureEnabled(FeatureType.Chat) && this.pluginInfo.isBungeeCord()){
		    
			List<String> bypassList = this.configManager.getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }
			
		    final String mcMessage = EmojiParser.parseToAliases(this.configManager.getMinecraftMessage("discordChatMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));
		    
	   		for(String server : this.configManager.getConfig().getConfigurationSection("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat").getKeys(false)) {
	   			long channelID = this.configManager.getConfig().getLong("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + server);
	   			
	   			if(channelID == channelId) {
	   				//CHANNEL WAS FOUND ON SEPERATED CHANNEL SETUP
	   				
					net.md_5.bungee.api.ProxyServer.getInstance().getPlayers().forEach(all -> {
					    	
						if(!all.getServer().getInfo().getName().equalsIgnoreCase(server))
					    	return;

		   				if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
							UUID uuidAll = all.getUniqueId();
							if(this.discordChatEnabled.containsKey(uuidAll) && this.discordChatEnabled.get(uuidAll)) {
								all.sendMessage(new TextComponent(mcMessage));
							}
		   				}else
							all.sendMessage(new TextComponent(mcMessage));
		   				
					});
	   				return;
	   			}
	   		}
	   	}
	    
		//STAFF CHAT
	    if(this.configManager.getChannelID(FeatureType.Staff) == channelId) {
	    	
			if(!this.configManager.isFeatureEnabled(FeatureType.Staff))
				return;
		    
			List<String> bypassList = this.configManager.getConfig().getStringList("Options." + FeatureType.Staff.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }

		    final String mcMessage = EmojiParser.parseToAliases(this.configManager.getMinecraftMessage("discordStaffMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));
		    
			  
		    if(this.pluginInfo.isBungeeCord()) {
		    	//ALL PLAYERS INGAME
		    	net.md_5.bungee.api.ProxyServer.getInstance().getPlayers().forEach(all -> {
					UUID uuidAll = all.getUniqueId();
					if(all.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
						if(!this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
							all.sendMessage(new TextComponent(mcMessage));
						}
					}
				});
		    }else if(!this.pluginInfo.isBungeeCordSubServer()){
				//ALL PLAYERS INGAME
				org.bukkit.Bukkit.getOnlinePlayers().forEach(all -> {
					UUID uuidAll = all.getUniqueId();
					if(all.hasPermission(this.configManager.getConfig().getString("Permissions.StaffChat"))) {
						if(!this.staffChatDisabled.containsKey(uuidAll) || !this.staffChatDisabled.get(uuidAll)) {
							all.sendMessage(mcMessage);
						}
					}
				});
		    }
		}
    }
}
