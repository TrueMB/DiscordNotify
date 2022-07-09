package me.truemb.discordnotify.utils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.spicord.Spicord;
import org.spicord.SpicordLoader;
import org.spicord.bot.DiscordBot;

import me.truemb.discordnotify.discord.commands.DC_PlayerInfoCommand;
import me.truemb.discordnotify.discord.commands.DC_VerifyCommand;
import me.truemb.discordnotify.discord.listener.DC_BroadcastListener;
import me.truemb.discordnotify.discord.listener.DC_ChatListener;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.MinotarTypes;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class DiscordManager {
	
	private DiscordBot discordBot = null;
	private boolean discordBotHooked = false;
	private int hookSchedulerId = -1; //IS THE SCHEDULER ID, WHICH HOOKS INTO THE DISCORD BOT

	private DiscordNotifyMain instance;
	
	//ADDONS
	private DC_PlayerInfoCommand playerInfoAddon;
	private DC_VerifyCommand verifyAddon;
	
	//LISTENER
	private DC_ChatListener chatListener;
	private DC_BroadcastListener broadcastListener;
	
	public DiscordManager(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

	//DISCORD
	public void registerAddons(String botname) {
    	
    	//ADDONS NEEDS TO BE SET UP IN SPICORD
	    this.playerInfoAddon = new DC_PlayerInfoCommand(this.instance);
	    this.verifyAddon = new DC_VerifyCommand(this.instance);
	    	
	    SpicordLoader.addStartupListener(spicord -> {
	    	
	    	DiscordBot bot = spicord.getBotByName(botname);
	    	
	    	if(bot == null) {
        		this.instance.getUniversalServer().getLogger().warning("Couldn't get Bot of the name: " + botname + ". (Did you change the botname in the config?)");
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
        	
        	//SEND SHUTDOWN MESSAGE TO DISCORD
    	    if(!this.instance.getUniversalServer().isProxy() && !this.instance.getUniversalServer().isProxySubServer())
    	    	this.announceServerStatus(false);
        	
        	//ADDONS
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.playerInfoAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.playerInfoAddon);
        	
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.verifyAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.verifyAddon);
        	
        	//LISTENER
            this.getDiscordBot().getJda().removeEventListener(this.chatListener);
            this.getDiscordBot().getJda().removeEventListener(this.broadcastListener);
        	
        	//SHUTDOWN
        	this.getDiscordBot().getJda().shutdownNow();
    		this.instance.getUniversalServer().getLogger().info("Disconnected from Discord BOT.");
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
	    	this.instance.getUniversalServer().getLogger().warning("Discord Bot is not Connected with a Server.");
	    	return;
	    }
	
	    //REGISTER LISTENER
		this.chatListener = new DC_ChatListener(this.instance);
		this.broadcastListener = new DC_BroadcastListener(this.instance);
		
	    this.getDiscordBot().getJda().addEventListener(this.chatListener);
	    this.getDiscordBot().getJda().addEventListener(this.broadcastListener);

    	//SEND START MESSAGE TO DISCORD
	    if(!this.instance.getUniversalServer().isProxy() && !this.instance.getUniversalServer().isProxySubServer())
	    	this.announceServerStatus(true);
	    	
		this.instance.getUniversalServer().getLogger().info("Connected with Discord BOT.");
		
	}
	
	private void announceServerStatus(boolean status) {
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.ServerStatus); //CANT BE SEPARATED SINCE IT ONLY TRIGGERS FOR NON NETWORKS

		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("server", "Server");
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.ServerStatus)) {
			this.instance.getDiscordManager().sendEmbedMessageWithNoPictureSync(channelId, status ? "ServerStartEmbed" : "ServerStopEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessageSync(channelId, status ? "ServerStartMessage" : "ServerStopMessage", placeholder);
		}
	}
	
	//GETS THE DISCORD BOT
	public DiscordBot getDiscordBot(){
		return this.discordBot;
	}

	/**
	 * 
	 * @param channelId - Channel to send the message to
	 * @param uuid - can be null - only interesting for Minotar Picture
	 * @param path
	 * @param placeholder
	 */
	public void sendEmbedMessage(long channelId, UUID uuid, String path, HashMap<String, String> placeholder) {
		if(this.getDiscordBot() == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

		new Thread(() -> {
			
			TextChannel channel = this.getDiscordBot().getJda().getTextChannelById(channelId);

			if(channel == null) {
				this.instance.getUniversalServer().getLogger().warning("Couldn't send Message to channel: " + channelId);
				return;
			}
			
			EmbedBuilder eb = this.getEmbedMessage(uuid, path, placeholder);
			
			//PICTURE ADDING TO MESSAGE - Only if uuid not null and Pictures are on
			if(uuid != null && this.instance.getConfigManager().getConfig().getBoolean("DiscordEmbedMessages." + path + ".WithPicture")) {
				String minotarTypeS = this.instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				InputStream file = null;
				String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".jpg";
				
				eb.setImage("attachment://" + filename);
				try {
					URL url = new URL("https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString());
					URLConnection urlConn = url.openConnection();
					file = urlConn.getInputStream();
				}catch (IOException e) {
					e.printStackTrace();
				}

				//SENDING MESSAGE WITH PICTURE
				if(file != null) {
					channel.sendMessage(eb.build()).addFile(file, filename).queue();
					return;
				}
			}
			
			//SENDS MESSAGE - if no Pictures are enabled
			channel.sendMessage(eb.build()).queue();
			
		}).start();
	}

	/**
	 * Doesn't Support Pictures!
	 * 
	 * @param channelId - Channel to send the message to
	 * @param uuid - can be null - only interesting for Minotar Picture
	 * @param path
	 * @param placeholder
	 */
	public void sendEmbedMessageWithNoPictureSync(long channelId, String path, HashMap<String, String> placeholder) {
		if(this.getDiscordBot() == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

		TextChannel channel = this.getDiscordBot().getJda().getTextChannelById(channelId);

		if(channel == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't send Message to channel: " + channelId);
			return;
		}
			
		EmbedBuilder eb = this.getEmbedMessage(null, path, placeholder);
		channel.sendMessage(eb.build()).complete();
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
		String title = this.instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".Title");
		String description = this.instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".Description");
		String author = this.instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".Author");
		
		title = this.getPlaceholderString(title, placeholder);
		description = this.getPlaceholderString(description, placeholder);
		author = this.getPlaceholderString(author, placeholder);

		//EMBED
		EmbedBuilder eb = new EmbedBuilder();

		if(author != null && !author.equalsIgnoreCase("")) {
			if(uuid != null && this.instance.getConfigManager().getConfig().getBoolean("DiscordEmbedMessages." + path + ".WithAuthorPicture"))
				eb.setAuthor(author, null, "https://minotar.net/" + "avatar" + "/" + uuid.toString());
			else
				eb.setAuthor(author);
		}

		if(title != null && !title.equalsIgnoreCase(""))
			eb.setTitle(title);

		if(description != null && !description.equalsIgnoreCase(""))
			eb.setDescription(description);
		
		List<String> fieldList = this.instance.getConfigManager().getConfig().getStringList("DiscordEmbedMessages." + path + ".Fields");
		if(fieldList != null) {
			for(String field : fieldList) {
				String[] array = field.split(" : ");
				String fieldTitle = array.length > 1 ? array[0] : "";
				String fieldBody = array.length > 1 ? array[1] : array[0];
				
				eb.addField(this.getPlaceholderString(fieldTitle, placeholder), this.getPlaceholderString(fieldBody, placeholder), true);
			}
		}
		
		Color color;
		try {
		    Field field = Color.class.getField(this.instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".Color").toUpperCase());
		    color = (Color)field.get(null);
		} catch (Exception e) {
		    color = null; // Not defined
		}

		eb.setColor(color);

		if(!this.instance.getConfigManager().getConfig().getBoolean("DiscordEmbedMessages." + path + ".DisableTimestamp"))
			eb.setTimestamp(Instant.now());
		
		return eb;
	}
	
	public String getDiscordMessage(String path, HashMap<String, String> placeholder) {
	   return this.getPlaceholderString(this.instance.getConfigManager().getConfig().getString("DiscordMessages." + path), placeholder);
    }

	public void sendDiscordMessageSync(long channelId, String path, HashMap<String, String> placeholder) {

		if(this.getDiscordBot() == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

	    TextChannel tc = this.getDiscordBot().getJda().getTextChannelById(channelId);
	    
	    if(tc == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find Channel with the ID: " + channelId);
	    	return;
	    }
	    tc.sendMessage(this.getDiscordMessage(path, placeholder)).complete();
    }
	
	public void sendDiscordMessage(long channelId, String path, HashMap<String, String> placeholder) {

		if(this.getDiscordBot() == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

	    TextChannel tc = this.getDiscordBot().getJda().getTextChannelById(channelId);
	    
	    if(tc == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find Channel with the ID: " + channelId);
	    	return;
	    }
	    tc.sendMessage(this.getDiscordMessage(path, placeholder)).queue();
    }
	
	public void sendPrivateDiscordMessage(User user, String message) {

		if(this.getDiscordBot() == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
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
		return this.discordBotHooked;
	}

	public int getHookSchedulerId() {
		return this.hookSchedulerId;
	}

	public void setHookSchedulerId(int hookSchedulerId) {
		this.hookSchedulerId = hookSchedulerId;
	}
	
}
