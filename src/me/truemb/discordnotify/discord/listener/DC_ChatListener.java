package me.truemb.discordnotify.discord.listener;

import java.util.List;
import java.util.UUID;

import com.vdurmont.emoji.EmojiParser;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DC_ChatListener extends ListenerAdapter {
	
	private DiscordNotifyMain instance;
	
	public DC_ChatListener(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
    	
        long channelId = e.getChannel().getIdLong();
        TextChannel channel = e.getTextChannel();
        String channelName =  EmojiParser.removeAllEmojis(channel.getName()).replace("[^a-zA-Z0-9 -]", "");

	    String message = e.getMessage().getContentDisplay();
	    
	    //WONT SEND MESSAGE OF DISCORD BOTS. SINCE THEY COULD BE A BAN COMMAND OR SO ON
	    if(e.getAuthor().isBot())
	    	return;
	    	   	    
	    //CORRECT CHANNEL
	    if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat") && this.instance.getConfigManager().getChannelID(FeatureType.Chat) == channelId) {
	    	
			if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat))
				return;
		    
			List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }
			
		    final String mcMessage = EmojiParser.parseToAliases(this.instance.getConfigManager().getMinecraftMessage("discordChatMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));

		  
		    if(!this.instance.getUniversalServer().isProxySubServer()) {

				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
					this.instance.getUniversalServer().getOnlinePlayers().forEach(all -> {
						UUID uuidAll = all.getUUID();
						if(this.instance.getDiscordChatEnabled().containsKey(uuidAll) && this.instance.getDiscordChatEnabled().get(uuidAll))
							all.sendMessage(mcMessage);
					});
				}else
					this.instance.getUniversalServer().broadcast(mcMessage);
				
		    }
	    	
	   	}else if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat) && this.instance.getUniversalServer().isProxy()){
		    
			List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }
			
		    final String mcMessage = EmojiParser.parseToAliases(this.instance.getConfigManager().getMinecraftMessage("discordChatMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));
		    
	   		for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat").getKeys(false)) {
	   			long channelID = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + server);
	   			
	   			if(channelID == channelId) {
	   				//SERVER SEPERATED CHANNEL

					this.instance.getUniversalServer().getOnlinePlayers().forEach(all -> {
					    	
						if(!all.getServer().equalsIgnoreCase(server))
					    	return;

		   				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
							UUID uuidAll = all.getUUID();
							if(this.instance.getDiscordChatEnabled().containsKey(uuidAll) && this.instance.getDiscordChatEnabled().get(uuidAll))
								all.sendMessage(mcMessage);
		   				}else
							all.sendMessage(mcMessage);
		   				
					});
	   				return;
	   			}
	   		}
	   	}
	    
		//STAFF CHAT
	    if(this.instance.getConfigManager().getChannelID(FeatureType.Staff) == channelId) {
	    	
			if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff))
				return;
		    
			List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Staff.toString() + ".bypassPrefix");
			for(String prefix : bypassList) {
		    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
		    		return;
		    }

		    final String mcMessage = EmojiParser.parseToAliases(this.instance.getConfigManager().getMinecraftMessage("discordStaffMessage", true)
		    		.replace("%Tag%", e.getAuthor().getAsTag())
		    		.replace("%Message%", message)
		    		.replace("%Channel%", channelName));
		    
			  
		    if(!this.instance.getUniversalServer().isProxySubServer()) {
		    	//ALL PLAYERS INGAME
				this.instance.getUniversalServer().getOnlinePlayers().forEach(all -> {
					UUID uuidAll = all.getUUID();
					if(all.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat")))
						if(!this.instance.getStaffChatDisabled().containsKey(uuidAll) || !this.instance.getStaffChatDisabled().get(uuidAll))
							all.sendMessage(mcMessage);
				});
		    }
		}
    }
}
