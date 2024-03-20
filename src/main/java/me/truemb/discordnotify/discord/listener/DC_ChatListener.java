package me.truemb.discordnotify.discord.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.vdurmont.emoji.EmojiParser;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.utils.JsonReader;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DC_ChatListener extends ListenerAdapter {
	
	private DiscordNotifyMain instance;
	
	private HashMap<String, Long> channel_id = new HashMap<>();
	
	public DC_ChatListener(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		this.getAllChannelIds();
	}

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
    	
        long channelId = e.getChannel().getIdLong();
        MessageChannelUnion channel = e.getChannel();
        String channelName =  EmojiParser.removeAllEmojis(channel.getName()).replace("[^a-zA-Z0-9 -]", "");

	    String message = e.getMessage().getContentDisplay();
	    
	    //WONT SEND MESSAGE OF DISCORD BOTS. SINCE THEY COULD BE A BAN COMMAND OR SO ON
	    if(e.getAuthor().isBot())
	    	return;
	    
	    HashMap<String, String> placeholder = new HashMap<>();
	    String tag = e.getAuthor().getAsTag();
	    String username = e.getAuthor().getName();
	    String nickname = e.getMember() != null && e.getMember().getNickname() != null ? e.getMember().getNickname() : "";
	    String name = e.getMember() != null && e.getMember().getEffectiveName() != null ? e.getMember().getEffectiveName() : "";
	    
	    placeholder.put("tag", tag);
	    placeholder.put("username", username);
	    placeholder.put("nickname", nickname);
	    placeholder.put("name", name);
	    	    
	    //CORRECT CHANNEL
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat)) {
		    
		    if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".syncDiscord"))
		    	return;
			
		    //CHECK IF IT IS A MANAGED CHANNEL
		    if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat")) {
		    	if(this.channel_id.containsKey(FeatureType.Chat.toString()) && this.channel_id.get(FeatureType.Chat.toString()) == channelId) {
				
			    	List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
					for(String prefix : bypassList) {
				    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
				    		return;
				    }
					
					//Verfied Feature enabled but the user is not verified
					if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".onlyVerified") && !this.instance.getVerifyManager().isVerified(e.getAuthor().getIdLong())) {
						e.getMessage().delete().queue();
				    	e.getMember().getUser().openPrivateChannel()
				    		.flatMap(pchannel -> pchannel.sendMessage(this.instance.getDiscordManager().getDiscordMessage("UserNotVerified", placeholder)))
				    		.queue(null, new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, (ex) -> System.out.print(""))); //prevent Error Message, so there wont be console spamming, if a user has private message enabled
						return;
					}
					
				    final String mcMessage = this.instance.getConfigManager().getMinecraftMessage("discordChatMessage", true)
				    		.replaceAll("(?i)%" + "tag" + "%", tag)
				    		.replaceAll("(?i)%" + "username" + "%", username)
				    		.replaceAll("(?i)%" + "nickname" + "%", nickname)
				    		.replaceAll("(?i)%" + "name" + "%", name)
				    		.replaceAll("(?i)%" + "message" + "%", EmojiParser.parseToAliases(message).replace("$", "\\$"))
				    		.replaceAll("(?i)%" + "channel" + "%", channelName);
		
				  
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
			    }
		    	
		   	}else if(this.instance.getUniversalServer().isProxy()){
		   						
			    final String mcMessage = this.instance.getConfigManager().getMinecraftMessage("discordChatMessage", true)
			    		.replaceAll("(?i)%" + "tag" + "%", tag)
			    		.replaceAll("(?i)%" + "username" + "%", username)
			    		.replaceAll("(?i)%" + "nickname" + "%", nickname)
			    		.replaceAll("(?i)%" + "name" + "%", name)
			    		.replaceAll("(?i)%" + "message" + "%", EmojiParser.parseToAliases(message).replace("$", "\\$"))
			    		.replaceAll("(?i)%" + "channel" + "%", channelName);
			    
		   		for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat").getKeys(false)) {
		   			String id = FeatureType.Chat.toString() + "_" + server.toLowerCase();
		   			if(!this.channel_id.containsKey(id))
		   				continue;
		   			
		   			long channelID = this.channel_id.get(id);
		   			
		   			if(channelID == channelId) {
		   				//SERVER SEPERATED CHANNEL

				   		List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Chat.toString() + ".bypassPrefix");
						for(String prefix : bypassList) {
					    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
					    		return;
					    }
						
						//Verfied Feature enabled but the user is not verified
						if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".onlyVerified") && !this.instance.getVerifyManager().isVerified(e.getAuthor().getIdLong())) {
							e.getMessage().delete().queue();
					    	e.getMember().getUser().openPrivateChannel()
					    		.flatMap(pchannel -> pchannel.sendMessage(this.instance.getDiscordManager().getDiscordMessage("UserNotVerified", placeholder)))
					    		.queue(null, new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, (ex) -> System.out.print(""))); //prevent Error Message, so there wont be console spamming, if a user has private message disabled
							return;
						}
	
						this.instance.getUniversalServer().getOnlinePlayers().forEach(all -> {
						    	
							if(all.getServer().equalsIgnoreCase(server)) {
				   				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat")) {
									UUID uuidAll = all.getUUID();
									if(this.instance.getDiscordChatEnabled().containsKey(uuidAll) && this.instance.getDiscordChatEnabled().get(uuidAll))
										all.sendMessage(mcMessage);
				   				}else
									all.sendMessage(mcMessage);
							}
			   				
						});
		   				break;
		   			}
		   		}
		   	}
		}

		//STAFF CHAT
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			
		    if(this.channel_id.containsKey(FeatureType.Staff.toString()) && this.channel_id.get(FeatureType.Staff.toString()) == channelId) {
		    			    
			    if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Staff.toString() + ".syncDiscord"))
			    	return;
			    
				List<String> bypassList = this.instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Staff.toString() + ".bypassPrefix");
				for(String prefix : bypassList) {
			    	if(message.toLowerCase().startsWith(prefix.toLowerCase()))
			    		return;
			    }
	
			    final String mcMessage = this.instance.getConfigManager().getMinecraftMessage("discordStaffMessage", true)
			    		.replaceAll("(?i)%" + "tag" + "%", tag)
			    		.replaceAll("(?i)%" + "username" + "%", username)
			    		.replaceAll("(?i)%" + "nickname" + "%", nickname)
			    		.replaceAll("(?i)%" + "name" + "%", name)
			    		.replaceAll("(?i)%" + "message" + "%", EmojiParser.parseToAliases(message).replace("$", "\\$"))
			    		.replaceAll("(?i)%" + "channel" + "%", channelName);
			    
				  
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
    
    
    private long lookupWebhookUrlToChannelId(String url) throws JSONException, IOException {
    	JSONObject json = JsonReader.readJsonFromUrl(url);
    	long channelId = json.getLong("channel_id");
    	
		return channelId;
    }
    
    private long convertId(String channelIdAsString) {
    	try {
			long channelId = Long.parseLong(channelIdAsString);
			return channelId;
		}catch (NumberFormatException ex) {
	    	try {
				return this.lookupWebhookUrlToChannelId(channelIdAsString);
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
		}
    	
    	return -1;
    }
    
    private void getAllChannelIds() {
    	this.instance.getUniversalServer().getLogger().info("Loading all Chat/Staff Channel Id's.");

    	if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat)) {
	    	if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat")) {
	    		this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat").getKeys(false).forEach(servers -> {
	    	    	String id = FeatureType.Chat.toString() + "_" + servers.toLowerCase();
	    	    	
	    			String channelIdAsString = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + servers);
	    			long channelId = this.convertId(channelIdAsString);
	    			
	    			if(channelId > 0)
		    			this.channel_id.put(id, channelId);
	
	    		});
	    	}else {
	    		String id = FeatureType.Chat.toString();
		    	
				String channelIdAsString = this.instance.getConfigManager().getChannel(FeatureType.Chat);
				long channelId = this.convertId(channelIdAsString);
				
				if(channelId > 0)
	    			this.channel_id.put(id, channelId);
	    	}
    	}
    	
    	if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
    		String id = FeatureType.Staff.toString();
	    	
			String channelIdAsString = this.instance.getConfigManager().getChannel(FeatureType.Staff);
			long channelId = this.convertId(channelIdAsString);
			
			if(channelId > 0)
    			this.channel_id.put(id, channelId);
    	}
    	this.instance.getUniversalServer().getLogger().info("Chat/Staff Channel Id's are found.");
    }
}
