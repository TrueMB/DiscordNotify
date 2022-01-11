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
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class DiscordManager {
	
	private DiscordBot discordBot = null;
	private boolean discordBotHooked = false;
	private int hookSchedulerId = -1; //IS THE SCHEDULER ID, WHICH HOOKS INTO THE DISCORD BOT

	private ConfigManager configManager;
	private PermissionsAPI permsAPI;
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
	
	private boolean onlineMode;
	
	@SuppressWarnings("deprecation") //FOR ONLINE MODE BUNGEECORD
	public DiscordManager(ConfigManager configManager, PermissionsAPI permsAPI, PluginInformations pluginInfo, OfflineInformationManager offlineInfo, VerifyManager verifyManager, VerifySQL verifySQL,DelayManager delayManager, HashMap<UUID, Boolean> staffHash, HashMap<UUID, Boolean> discordChatHash) {
		this.configManager = configManager;
		this.permsAPI = permsAPI;
		this.pluginInfo = pluginInfo;
		this.offlineInfo = offlineInfo;
		this.verifyManager = verifyManager;
		this.delayManager = delayManager;
		this.verifySQL = verifySQL;
		
		this.staffHash = staffHash;
		this.discordChatHash = discordChatHash;

		//ONLINE MODE NEEDED FOR SPICORD STUFF. THATSWHY IT IS HERE
		if(pluginInfo.isBungeeCord()) {
			this.onlineMode = net.md_5.bungee.api.ProxyServer.getInstance().getConfig().isOnlineMode();
		}else {
			this.onlineMode = org.bukkit.Bukkit.getOnlineMode();
		}
	}

	//DISCORD
	public void registerAddons(String botname) {
		
		//ADDONS
    	if(Spicord.getInstance() == null || Spicord.getInstance().getAddonManager() == null)
    		return;
    	
    	//ADDONS NEEDS TO BE SET UP IN SPICORD
	    this.playerInfoAddon = new DC_PlayerInfoCommand(this, this.configManager, this.offlineInfo, this.pluginInfo);
	    this.verifyAddon = new DC_VerifyCommand(this.pluginInfo, this.permsAPI, this, this.verifyManager, this.delayManager, this.configManager, this.verifySQL);
	    	
	    SpicordLoader.addStartupListener(spicord -> {
	    	
	    	DiscordBot bot = spicord.getBotByName(botname);
	    	
	    	if(bot == null) {
        		this.pluginInfo.getLogger().warning("Couldnt get Bot of the name: " + botname + ". (Did you change the botname in the config?)");
        		return;
	    	}
	    	
    		setDiscordBot(bot);
    		
	    	//REGISTER ADDONS
	    	spicord.getAddonManager().registerAddon(this.playerInfoAddon);
	    	spicord.getAddonManager().registerAddon(this.verifyAddon);
	    });
	    
	}
	
	public void disconnectDiscordBot() {
        if(this.getDiscordBot() != null && this.getDiscordBot().isReady()) {
        	
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
	
	public void prepareDiscordBot() {
			
		if(this.discordBot == null || !this.discordBot.isReady())
	    	return;
			
		//Discord bot is reachable. If it now crashes, then there went something else wrong, which is not fixable without user interaction.
		this.discordBotHooked = true;
		
	    //CHECK IF BOT IS CONNECTED TO A GUILD
	    if(this.discordBot.getJda().getGuilds().size() <= 0) {
	    	this.pluginInfo.getLogger().warning("Discord Bot is not Connected with a Server.");
	    	return;
	    }
	
	    //REGISTER LISTENER
		this.chatListener = new DC_ChatListener(this.configManager, this.pluginInfo, this.staffHash, this.discordChatHash);
	    this.getDiscordBot().getJda().addEventListener(this.chatListener);
	    	
		this.pluginInfo.getLogger().info("Connected with Discord BOT.");
		
	}
	
	//GETS THE DISCORD BOT
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
		String minotarTypeS = this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".PictureType");
		MinotarTypes minotarType = MinotarTypes.BUST;
		try {
			minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
		}catch(Exception ex) { /* NOTING */ }
		
		InputStream file = null;
		String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".jpg";
		
		if(this.configManager.getConfig().getBoolean("DiscordEmbedMessages." + path + ".WithPicture")) {
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
		String title = this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".Title");
		String description = this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".Description");
		String author = this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".Author");
		
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
		
		List<String> fieldList = this.configManager.getConfig().getStringList("DiscordEmbedMessages." + path + ".Fields");
		if(fieldList != null) {
			for(String field : fieldList) {
				String[] array = field.split(" : ");
				String fieldTitle = array.length > 1 ? array[0] : "";
				String fieldBody = array.length > 1 ? array[1] : array[0];
				
				eb.addField(this.getPlaceholderString(fieldTitle, placeholder), this.getPlaceholderString(fieldBody, placeholder), true);
			}
		}
		
		eb.setColor(Color.getColor(this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".Color").toUpperCase()));
		eb.setTimestamp(Instant.now());
		
		return eb;
	}
	
	public String getDiscordMessage(String path, HashMap<String, String> placeholder) {
	   return this.getPlaceholderString(this.configManager.getConfig().getString("DiscordMessages." + path), placeholder);
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

	public boolean isDiscordBotHooked() {
		return discordBotHooked;
	}

	public int getHookSchedulerId() {
		return hookSchedulerId;
	}

	public void setHookSchedulerId(int hookSchedulerId) {
		this.hookSchedulerId = hookSchedulerId;
	}

	public boolean isOnlineMode() {
		return onlineMode;
	}
	
}
