package me.truemb.discordnotify.utils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.spicord.Spicord;
import org.spicord.SpicordLoader;
import org.spicord.bot.DiscordBot;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.WebhookCluster;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import me.truemb.discordnotify.discord.commands.DC_PlayerInfoCommand;
import me.truemb.discordnotify.discord.commands.DC_VerifyCommand;
import me.truemb.discordnotify.discord.listener.DC_BroadcastListener;
import me.truemb.discordnotify.discord.listener.DC_ChatListener;
import me.truemb.discordnotify.discord.listener.DC_RoleChangeListener;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.MinotarTypes;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.staticembed.StaticEmbedManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.OkHttpClient;

public class DiscordManager {

    public static final Pattern WEBHOOK_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?discord\\.com/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");
    
	private DiscordBot discordBot = null;
	private boolean discordBotHooked = false;
	private int hookSchedulerId = -1; //IS THE SCHEDULER ID, WHICH HOOKS INTO THE DISCORD BOT
	
	private Guild guild;
	private WebhookCluster webookCluster;
	private HashMap<String, WebhookClient> webhookClients = new HashMap<>();

	private DiscordNotifyMain instance;
	
	private StaticEmbedManager staticEmbedManager;
	
	//ADDONS
	private DC_PlayerInfoCommand playerInfoAddon;
	private DC_VerifyCommand verifyAddon;
	
	//LISTENER
	private DC_ChatListener chatListener;
	private DC_BroadcastListener broadcastListener;
	private DC_RoleChangeListener roleChangeListener;
	
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
        	
        	this.staticEmbedManager.shutdown();
        	
        	//SEND SHUTDOWN MESSAGE TO DISCORD
        	if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.ServerStatus))
        		if(!this.instance.getUniversalServer().isProxySubServer())
        			this.announceServerStatus(false);
        	
        	//ADDONS
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.playerInfoAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.playerInfoAddon);
        	
        	if(Spicord.getInstance().getAddonManager().isRegistered(this.verifyAddon))
        		Spicord.getInstance().getAddonManager().unregisterAddon(this.verifyAddon);
        	
        	//LISTENER
            this.getDiscordBot().getJda().removeEventListener(this.chatListener);
            this.getDiscordBot().getJda().removeEventListener(this.broadcastListener);
            this.getDiscordBot().getJda().removeEventListener(this.roleChangeListener);
        	
        	//SHUTDOWN
        	this.getDiscordBot().getJda().shutdownNow();
    		
    		this.webookCluster.close();
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
	    
	    //Create Cluster
	    this.createCluster();
	    
		long discordServerId = this.instance.getConfigManager().getConfig().getLong("Options.DiscordBot.ServerID");
		this.guild = discordServerId <= 0 ? this.discordBot.getJda().getGuilds().get(0) : this.discordBot.getJda().getGuildById(discordServerId);
	
	    //REGISTER LISTENER
		this.chatListener = new DC_ChatListener(this.instance);
		this.broadcastListener = new DC_BroadcastListener(this.instance);
		this.roleChangeListener = new DC_RoleChangeListener(this.instance);
		
	    this.getDiscordBot().getJda().addEventListener(this.chatListener);
	    this.getDiscordBot().getJda().addEventListener(this.broadcastListener);
	    this.getDiscordBot().getJda().addEventListener(this.roleChangeListener);
	    
	    this.staticEmbedManager = new StaticEmbedManager(this.instance);

    	//SEND START MESSAGE TO DISCORD
    	if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.ServerStatus))
    		if(!this.instance.getUniversalServer().isProxySubServer())
    			this.announceServerStatus(true);
	    	
		this.instance.getUniversalServer().getLogger().info("Connected with Discord BOT.");
		
	}
	
	private void announceServerStatus(boolean status) {
		String server = this.instance.getUniversalServer().isProxy() ? "Proxy" : "Server";
		long channelId;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.ServerStatus.toString() + ".enableServerSeperatedStatus"))
			channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.ServerStatus.toString() + ".serverSeperatedStatus." + server);
		else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.ServerStatus);

		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("server", server);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.ServerStatus)) {
			this.sendEmbedMessageWithNoPictureSync(channelId, status ? "ServerStartEmbed" : "ServerStopEmbed", placeholder);
		}else {
			this.sendDiscordMessageSync(channelId, status ? "ServerStartMessage" : "ServerStopMessage", placeholder);
		}
	}
	
	//GETS THE DISCORD BOT
	public DiscordBot getDiscordBot(){
		return this.discordBot;
	}
	
	public Guild getCurrentGuild(){
		return this.guild;
	}
	
	private void createCluster() {

		// Create and initialize the cluster
		this.webookCluster = new WebhookCluster(5); // create an initial 5 slots (dynamic like lists)
		this.webookCluster.setDefaultHttpClient(new OkHttpClient());
		this.webookCluster.setDefaultDaemon(true);
	}
	
	/**
	 * Need an own Method, since the Link isn't correct anymore in the API.
	 * 
	 * @param url
	 * @return
	 */
	private WebhookClientBuilder createWebhookClientBuilder(String url) {

        Objects.requireNonNull(url, "Url");
        Matcher matcher = WEBHOOK_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Failed to parse webhook URL");
        }

        long id = Long.parseUnsignedLong(matcher.group(1));
        String token = matcher.group(2);
        
        return new WebhookClientBuilder(id, token);
	}

	public WebhookClient createOrLoadWebhook(FeatureType type, String url) {
		return this.createOrLoadWebhook(type, null, url);
	}
	
	public WebhookClient createOrLoadWebhook(FeatureType type, @Nullable String server, String url) {
		
		String id = server == null ? type.toString() : type.toString() + "_" + server;
		if(this.webhookClients.containsKey(id))
			return this.webhookClients.get(id);
		
		WebhookClientBuilder cbuilder = this.createWebhookClientBuilder(url);
		cbuilder.setThreadFactory((job) -> {
		    Thread thread = new Thread(job);
		    thread.setName("Webhook");
		    thread.setDaemon(true);
		    return thread;
		});
		cbuilder.setWait(true);
		
		// Build client
		WebhookClient client = cbuilder.build();
		
		// Add to cluster
		this.webookCluster.addWebhooks(client);
		this.webhookClients.put(id, client);
		
		return client;
	}
	
	public void sendWebhookMessage(WebhookClient client, String username, String avatarUrl, String content, HashMap<String, String> placeholders) {
		
		WebhookMessageBuilder builder = new WebhookMessageBuilder();
		
		builder.setUsername(this.getPlaceholderString(username, placeholders));
		builder.setAvatarUrl(avatarUrl);
		builder.setContent(this.getPlaceholderString(content, placeholders));
		
		client.send(builder.build());
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
				int size = this.instance.getConfigManager().getConfig().getInt("DiscordEmbedMessages." + path + ".PictureSize");
				if(size <= 0)
					size = 100;
				
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				InputStream file = null;
				String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".png";
				
				eb.setImage("attachment://" + filename);
				try {
					URL url = new URL("https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString() + "/" + String.valueOf(size) + ".png");
					URLConnection urlConn = url.openConnection();
					file = urlConn.getInputStream();
				}catch (IOException e) {
					e.printStackTrace();
				}

				//SENDING MESSAGE WITH PICTURE
				if(file != null) {
					channel.sendMessageEmbeds(eb.build()).addFiles(FileUpload.fromData(file, filename)).queue();
					return;
				}
			}
			
			//SENDS MESSAGE - if no Pictures are enabled
			channel.sendMessageEmbeds(eb.build()).queue();
			
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
		channel.sendMessageEmbeds(eb.build()).queue();
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
				eb.setAuthor(author, null, "https://minotar.net/" + "avatar" + "/" + uuid.toString() + "/100.png");
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
	
	
	//ROLE SYNC

	//ROLESYNC
	public void checkForRolesUpdate(UUID uuid, long disuuid, String[] currentGroupList) {
		if(this.instance.getDiscordManager() == null)
			return;
		
		DiscordBot discordBot = this.getDiscordBot();
		if(discordBot == null) 
			return;

		long discordServerId = this.instance.getConfigManager().getConfig().getLong("Options.DiscordBot.ServerID");
		Guild guild = discordServerId <= 0 ? discordBot.getJda().getGuilds().get(0) : discordBot.getJda().getGuildById(discordServerId);
		
		Member member = guild.getMemberById(disuuid);

		if(member == null) {
			guild.retrieveMemberById(disuuid).queue(mem -> {
				this.checkForRolesUpdate(uuid, mem, currentGroupList);
			});
		}else
			this.checkForRolesUpdate(uuid, member, currentGroupList);
	}

	//CHECK FOR UPDATES
	public void checkForRolesUpdate(UUID uuid, Member member, String[] currentGroupList) {
		if(this.instance.getDiscordManager() == null) 
			return;
		
		DiscordBot discordBot = this.getDiscordBot();
		if(discordBot == null) 
			return;

		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
			return;
		
		//NOT CORRECTLY VERIFIED
		if(!this.instance.getVerifyManager().isVerified(uuid) || this.instance.getVerifyManager().getVerfiedWith(uuid) != member.getIdLong())
			return;
		
		boolean changesWereMade = false;

		List<String> rolesBackup = this.instance.getVerifyManager().getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();

		for(String group : currentGroupList) {
			List<Role> roles = new ArrayList<>();
			if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = discordBot.getJda().getRolesByName(group, true);
			else {
				String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + group.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = discordBot.getJda().getRolesByName(groupConfig, true);
			}
			
			if(roles.size() <= 0)
				continue;
			
			Role role = roles.get(0);
			String roleName = role.getName();

			if(rolesBackup.contains(roleName))
				continue;
				
			rolesBackup.add(roleName);
			role.getGuild().addRoleToMember(member, role).queue();
			changesWereMade = true;
		}
		
		List<String> allBackupRoles = new ArrayList<>(rolesBackup);
		for(String backupRoles : allBackupRoles) {
			boolean isInGroup = false;
			for(String group : currentGroupList) {
				if(backupRoles.equalsIgnoreCase(group)) {
					isInGroup = true;
				}
			}
			if(!isInGroup) {
				List<Role> roles = new ArrayList<>();
				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
					roles = discordBot.getJda().getRolesByName(backupRoles, true);
				else {
					String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
					
					if(groupConfig == null)
						continue;
					
					roles = discordBot.getJda().getRolesByName(groupConfig, true);
				}
				if(roles.size() <= 0)
					continue;
				
				Role role = roles.get(0);
				String roleName = role.getName();

				role.getGuild().removeRoleFromMember(member, role).queue();
				rolesBackup.remove(roleName);
				
				changesWereMade = true;
			}
		}

		this.instance.getVerifyManager().setBackupRoles(uuid, rolesBackup);
		if(changesWereMade)
			this.instance.getVerifySQL().updateRoles(uuid, rolesBackup);
	}

	public void resetRoles(UUID uuid, Member member) {
		if(this.instance.getDiscordManager() == null) 
			return;
		
		DiscordBot discordBot = this.getDiscordBot();
		if(discordBot == null) 
			return;
		
		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
			return;

		List<String> rolesBackup = this.instance.getVerifyManager().getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();
		
		for(String backupRoles : rolesBackup) {
			List<Role> roles = new ArrayList<>();
			if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = discordBot.getJda().getRolesByName(backupRoles, true);
			else {
				String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = discordBot.getJda().getRolesByName(groupConfig, true);
			}
			if(roles.size() <= 0)
				continue;
				
			Role role = roles.get(0);
			role.getGuild().removeRoleFromMember(member, role).complete();
		}

		this.instance.getVerifyManager().removeBackupRoles(uuid);
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
		message = message.replace("%n", System.lineSeparator());
		
		return message;
	}

	public boolean isDiscordBotHooked() {
		return this.discordBotHooked;
	}

	public int getHookSchedulerId() {
		return this.hookSchedulerId;
	}
	
	public StaticEmbedManager getStaticEmbedManager() {
		return this.staticEmbedManager;
	}

	public void setHookSchedulerId(int hookSchedulerId) {
		this.hookSchedulerId = hookSchedulerId;
	}
	
}
