package me.truemb.disnotify.utils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.spicord.Spicord;
import org.spicord.SpicordLoader;
import org.spicord.bot.DiscordBot;

import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.discord.commands.DC_PlayerInfoCommand;
import me.truemb.disnotify.discord.commands.DC_VerifyCommand;
import me.truemb.disnotify.discord.listener.DC_ChatListener;
import me.truemb.disnotify.enums.MinotarTypes;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class DiscordManager {
	
	//DOWNLOAD PICTURE TO PREVENT DISCORD CACHE
	//FIXED NEVER PLAYED VALUE
	//TODO CONFIG NEEDS TO REMOVE hours FROM playerinfo embed
	
	private DiscordBot discordBot = null;

	private ConfigCacheHandler configCache;
	private PluginInformations pluginInfo;
	private OfflineInformationManager offlineInfo;
	private VerifyManager verifyManager;
	private DelayManager delayManager;
	private VerifySQL verifySQL;
	
	//ADDONS
	private DC_PlayerInfoCommand playerInfoAddon;
	private DC_VerifyCommand verifyAddon;
	
	//LISTENER
	private DC_ChatListener chatListener;
	
	//STAFF HASH
	private HashMap<UUID, Boolean> staffHash;
	private HashMap<UUID, Boolean> discordChatHash;
	
	public DiscordManager(ConfigCacheHandler configCache, PluginInformations pluginInfo, OfflineInformationManager offlineInfo, VerifyManager verifyManager, VerifySQL verifySQL,DelayManager delayManager, HashMap<UUID, Boolean> staffHash, HashMap<UUID, Boolean> discordChatHash) {
		this.configCache = configCache;
		this.pluginInfo = pluginInfo;
		this.offlineInfo = offlineInfo;
		this.verifyManager = verifyManager;
		this.delayManager = delayManager;
		this.verifySQL = verifySQL;
		
		this.staffHash = staffHash;
		this.discordChatHash = discordChatHash;
		
		this.registerAddons();
	}

	//DISCORD
	private void registerAddons() {
		
		//ADDONS
    	if(Spicord.getInstance() == null || Spicord.getInstance().getAddonManager() == null)
    		return;
    	
    	//ADDONS NEEDS TO BE SET UP IN SPICORD
	    this.playerInfoAddon = new DC_PlayerInfoCommand(this, this.configCache, this.offlineInfo, this.pluginInfo);
	    this.verifyAddon = new DC_VerifyCommand(this.pluginInfo, this, this.verifyManager, this.delayManager, this.configCache, this.verifySQL);
	    	
	    SpicordLoader.addStartupListener(spicord -> {
	    	
	    	spicord.getAddonManager().registerAddon(this.playerInfoAddon);
	    	spicord.getAddonManager().registerAddon(this.verifyAddon);
	    	
	    });
	}
	
	public void prepareDiscordBot(DiscordBot bot) {
        if(this.getDiscordBot() == null) {
        	
        	//CHECK IF BOT IS CONNECTED TO A GUILD
        	if(bot.getJda().getGuilds().size() <= 0) {
        		this.pluginInfo.getLogger().warning("Discord Bot is not Connected with a Server.");
        		return;
        	}
        	
        	//SET THE BOT
        	this.setDiscordBot(bot);

        	//REGISTER LISTENER
    	    this.chatListener = new DC_ChatListener(this.configCache, this.pluginInfo, this.staffHash, this.discordChatHash);
            this.getDiscordBot().getJda().addEventListener(this.chatListener);
        	
    		this.pluginInfo.getLogger().info("Connected with Discord BOT.");
        }
	}
	
	public void disconnectDiscordBot() {
        if(this.getDiscordBot() != null) {
        	
        	//ADDONS
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.playerInfoAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.playerInfoAddon);
        	
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.verifyAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.verifyAddon);
        	
        	//LISTENER
            this.getDiscordBot().getJda().removeEventListener(this.chatListener);
        	
        	//SHUTDOWN
        	this.getDiscordBot().getJda().shutdownNow();
    		this.pluginInfo.getLogger().info("Disconnected from Discord BOT.");
        }
	}

	//DISCOD BOT
	public void setDiscordBot(DiscordBot bot) {
		this.discordBot = bot;
	}
	
	public DiscordBot getDiscordBot(){
		return this.discordBot;
	}

	public void sendEmbedMessage(long channelId, UUID uuid, String path, HashMap<String, String> placeholder) {
		if(this.getDiscordBot() == null) {
    		this.pluginInfo.getLogger().warning("Discord BOT is not ready.");
			return;
		}
		
		TextChannel channel = this.getDiscordBot().getJda().getTextChannelById(channelId);
		
		if(this.getDiscordBot() == null) {
    		this.pluginInfo.getLogger().warning("The Channel with the ID: " + channelId + " doesn't exists.");
			return;
		}
		
		EmbedBuilder eb = this.getEmbedMessage(uuid, path, placeholder);
		
		//PICTURE ADDING TO MESSAGE
		String minotarTypeS = this.configCache.getEmbedString(path + ".PictureType");
		MinotarTypes minotarType = MinotarTypes.BUST;
		try {
			minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
		}catch(Exception ex) { /* NOTING */ }
		
		InputStream file = null;
		String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".jpg";
		
		if(this.configCache.getEmbedBoolean(path + ".WithPicture")) {
			eb.setImage("attachment://" + filename);

			try {
				URL url = new URL("https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString());
				URLConnection urlConn = url.openConnection();
				file = urlConn.getInputStream();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		//===========
		
		if(file != null)
			channel.sendMessage(eb.build()).addFile(file, filename).queue();
		else
			channel.sendMessage(eb.build()).queue();
	}
	
	public boolean isAddonEnabled(String addonName) {
		if(this.getDiscordBot() == null)
			return false;
		
		for(String addons : this.getDiscordBot().getAddons()) {
			if(addons.equalsIgnoreCase(addonName)) {
				return true;
			}
		}
		return false;
	}
	
	public EmbedBuilder getEmbedMessage(UUID uuid, String path, HashMap<String, String> placeholder) {
		
		//MESSAGES
		String title = this.configCache.getEmbedString(path + ".Title");
		String description = this.configCache.getEmbedString(path + ".Description");
		String author = this.configCache.getEmbedString(path + ".Author");
		
		title = this.getPlaceholderString(title, placeholder);
		description = this.getPlaceholderString(description, placeholder);
		author = this.getPlaceholderString(author, placeholder);
		
		//EMBED
		EmbedBuilder eb = new EmbedBuilder();

		if(author != null && !author.equalsIgnoreCase(""))
			eb.setAuthor(author);

		if(title != null && !title.equalsIgnoreCase(""))
			eb.setTitle(title);

		if(description != null && !description.equalsIgnoreCase(""))
			eb.setDescription(description);
		
		List<String> fieldList = this.configCache.getList("DiscordEmbedMessages." + path + ".Fields");
		if(fieldList != null) {
			for(String field : fieldList) {
				String[] array = field.split(" : ");
				String fieldTitle = array.length > 1 ? array[0] : "";
				String fieldBody = array.length > 1 ? array[1] : array[0];
				
				eb.addField(this.getPlaceholderString(fieldTitle, placeholder), this.getPlaceholderString(fieldBody, placeholder), true);
			}
		}
		
		eb.setColor(Color.getColor(this.configCache.getEmbedString(path + ".Color").toUpperCase()));
		eb.setTimestamp(Instant.now());
		
		return eb;
	}
	
	public String getDiscordMessage(String path, HashMap<String, String> placeholder) {
	   return this.getPlaceholderString(this.configCache.getDiscordMessage(path), placeholder);
    }
	
	public void sendDiscordMessage(long channelId, String path, HashMap<String, String> placeholder) {

		if(this.getDiscordBot() == null) {
    		this.pluginInfo.getLogger().warning("Discord BOT is not ready.");
			return;
		}

	    TextChannel tc = this.getDiscordBot().getJda().getTextChannelById(channelId);
	    
	    if(tc == null) {
			this.pluginInfo.getLogger().warning("Couldn't find Channel with the ID: " + channelId);
	    	return;
	    }
	    tc.sendMessage(this.getDiscordMessage(path, placeholder)).submit();
    }
	
	public void sendPrivateDiscordMessage(User user, String message) {

		if(this.getDiscordBot() == null) {
    		this.pluginInfo.getLogger().warning("Discord BOT is not ready.");
			return;
		}

		user.openPrivateChannel().queue((channel) -> {
			channel.sendMessage(message).submit();
		});
    }
	
	//PLACEHOLDERS
	public String getPlaceholderString(String message, HashMap<String, String> placeholder) {
		
		if(placeholder != null){
			for(String key : placeholder.keySet()) {
				String value = placeholder.get(key);
				if(value != null)
					message = message.replaceAll("(?i)%" + key.toLowerCase() + "%", value); //IGNORES UPPER CASE?
			}
		}
		
		return message;
	}
	
}
