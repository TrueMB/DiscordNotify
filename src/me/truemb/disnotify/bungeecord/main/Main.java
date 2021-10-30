package me.truemb.disnotify.bungeecord.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spicord.Spicord;

import me.truemb.disnotify.bungeecord.commands.BC_DChatCommand;
import me.truemb.disnotify.bungeecord.commands.BC_StaffCommand;
import me.truemb.disnotify.bungeecord.commands.BC_VerifyCommand;
import me.truemb.disnotify.bungeecord.listener.BC_ChatListener;
import me.truemb.disnotify.bungeecord.listener.BC_JoinLeaveListener;
import me.truemb.disnotify.bungeecord.listener.BC_JoinQuitGeneralListener;
import me.truemb.disnotify.database.AsyncMySQL;
import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.runnable.BC_InactivityChecker;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.ConfigUpdater;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PluginInformations;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Main extends Plugin{
	
	//MySQL
	private AsyncMySQL sql;
	private OfflineInformationsSQL offlineInfoSQL;
	private VerifySQL verifySQL;
	
	//DISCORD BOT
	private DiscordManager discordMGR;
    
    //MANAGER + HANDLER
	private ConfigCacheHandler configCache;
    private PluginInformations pluginInformations;
    private OfflineInformationManager offlineInformationManager;
    private DelayManager delayManger;
    private VerifyManager verifyManager;
    private PluginMessagingBungeecordManager messagingManager;
    
    private PermissionsAPI permsAPI;
	
	private int inactivityCheckerRunn = -1;
	public boolean placeholderSupport = false;
	private boolean isPluginEnabled = true;

	//DATA
	public HashMap<UUID, Long> joinTime = new HashMap<UUID, Long>();
	
	//CONFIG
	public static final int configVersion = 6;
	private File file;
	private Configuration config;
	
	
	//BUNGEECORD MAIN CLASS

	@Override
	public void onEnable() {
		
	    HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
	    HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
	    
		String serverVersion = this.getProxy().getVersion();
		
		//FILES
		this.manageFile(); //LOAD CONFIG FILE
		
		this.loadConfigCache();
		
		//Bungeecord Messaging Channel
		this.messagingManager = new PluginMessagingBungeecordManager(this);
		
		//MANAGERS
		this.pluginInformations = new PluginInformations(this.getDescription().getName(), this.getDescription().getVersion(), serverVersion, this.getLogger(), true, false);
		this.delayManger = new DelayManager();
		this.verifyManager = new VerifyManager(this.getDelayManger());
		this.offlineInformationManager = new OfflineInformationManager();
		
		//PERMISSIONS API
		this.permsAPI = new PermissionsAPI(this.getPluginInformations());

		//MySQL
		this.startMySql();
		
		if(!this.isPluginEnabled)
			return; //DATABASE MISSING
		
		//SPICORD
		this.discordMGR = new DiscordManager(this.getConfigCache(), this.getPluginInformations(), this.getOfflineInformationManager(), this.getVerifyManager(), this.getVerifySQL(), this.getDelayManger(), staffChatDisabled, discordChatEnabled);
		
		//LISTENER
		if(this.configCache.isFeatureEnabled(FeatureType.PlayerJoinLeave)) {
			BC_JoinLeaveListener joinQuitListener = new BC_JoinLeaveListener(this.getDiscordManager(), this.getConfigCache());
			this.getProxy().getPluginManager().registerListener(this, joinQuitListener);
		}

		if(this.configCache.isFeatureEnabled(FeatureType.Chat)) {
			BC_ChatListener chatListener = new BC_ChatListener(this.getDiscordManager(), this.getConfigCache(), discordChatEnabled);
			this.getProxy().getPluginManager().registerListener(this, chatListener);
		}
		
		//GENERAL JOIN QUIT FOR MULTIPLE STUFF
		BC_JoinQuitGeneralListener joinQuitListener = new BC_JoinQuitGeneralListener(this.getVerifyManager(), this.getMessagingManager(), this.getOfflineInformationsSQL(), this.joinTime);
		this.getProxy().getPluginManager().registerListener(this, joinQuitListener);
		//================================================================================
		
		//COMMANDS
		if(this.manageFile().getBoolean("Options.Chat.enableSplittedChat")) {
			BC_DChatCommand dchatCmd = new BC_DChatCommand(this.getConfigCache(), discordChatEnabled);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, dchatCmd);
		}
		
		if(this.manageFile().getBoolean("FeaturesEnabled.Staff")) {
			BC_StaffCommand staffCmd = new BC_StaffCommand(this.getDiscordManager(), this.getConfigCache(), staffChatDisabled);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, staffCmd);
		}
		
		BC_VerifyCommand verifyCmd = new BC_VerifyCommand(this.getDiscordManager(), this.getConfigCache(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getMessagingManager(), this.getPermissionsAPI());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, verifyCmd);
		
		//REGISTER BOT
		ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
			
			@Override
			public void run() {
				if(getDiscordManager().getDiscordBot() == null)
					getDiscordManager().prepareDiscordBot(Spicord.getInstance().getBotByName(manageFile().getString("Options.DiscordBot.Name")));
			}
		}, 12, TimeUnit.SECONDS);
		
		
		//If Server gets reloaded, it sets the current time again
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UUID uuid = all.getUniqueId();
			
			this.joinTime.put(uuid, System.currentTimeMillis());
		}
		
		//RUNNABLES
		if(this.manageFile().getBoolean("FeaturesEnabled.Inactivity"))
			this.inactivityCheckerRunn = ProxyServer.getInstance().getScheduler().schedule(this, new BC_InactivityChecker(this.getDiscordManager(), this.getPluginInformations(), this.getConfigCache(), this.getAsyncMySql(), this.getOfflineInformationsSQL()), 15, 60 * this.manageFile().getInt("Options.Inactivity.CheckTimer"), TimeUnit.SECONDS).getId();
		
	}
	
	@Override
	public void onDisable() {
		
		//If Server gets reloaded or stoped, data gets saved
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UUID uuid = all.getUniqueId();
			
			if(this.joinTime.get(uuid) != null) {
				long time = System.currentTimeMillis() - this.joinTime.get(uuid);
				
				this.offlineInfoSQL.addToInformation(uuid, InformationType.Playtime, time);
				this.offlineInfoSQL.getOfflineInfoManager().addInformation(uuid, InformationType.Playtime, time);
				this.messagingManager.sendInformationUpdate(all, InformationType.Playtime, this.offlineInfoSQL.getOfflineInfoManager().getInformationLong(uuid, InformationType.Playtime));
				
				this.joinTime.remove(uuid);
			}
		}
		
		if(this.inactivityCheckerRunn > 0)
			ProxyServer.getInstance().getScheduler().cancel(this.inactivityCheckerRunn);
		
		if(this.getDiscordManager() != null)
			this.getDiscordManager().disconnectDiscordBot();
	}
	
	//MySQL
	private void startMySql() {
		this.getLogger().info("{MySQL}  starting MySQL . . .");

		String host = this.manageFile().getString("Database.host");
		int port = this.manageFile().getInt("Database.port");
		String user = this.manageFile().getString("Database.user");
		String password = this.manageFile().getString("Database.password");
		String database = this.manageFile().getString("Database.database");
		boolean useSSL = this.manageFile().getBoolean("Database.useSSL");
		
		try {
			this.sql = new AsyncMySQL(this.getLogger(), host, port, user, password, database, useSSL);
			this.offlineInfoSQL = new OfflineInformationsSQL(this.getAsyncMySql(), this.getOfflineInformationManager());
			this.verifySQL = new VerifySQL(this.getAsyncMySql(), this.getVerifyManager(), this.getConfigCache(), this.getPluginInformations());
			
			ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
				
				@Override
				public void run() {
					getOfflineInformationsSQL().setup();
					getVerifySQL().setupVerifications();
				}
			}, 2, TimeUnit.SECONDS);
			
			this.getLogger().info("{MySQL}  successfully connected to Database.");
		} catch (Exception e) {
			this.getLogger().warning("{MySQL}  Failed to start MySql (" + e.getMessage() + ")");
			this.isPluginEnabled = false;
		}
	}

	//CONFIG
	public Configuration manageFile() {
		File directory = new File(this.getDataFolder().getPath());
		File configFile = this.getConfigFile();
		if(configFile == null)
			this.file = configFile = new File(directory, "config.yml");
		
		if (!configFile.exists()){
			
			if(!directory.exists())
				directory.mkdir();
			
			try (InputStream in = this.getResourceAsStream("config.yml")) {
		        Files.copy(in, file.toPath());
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
		if(this.config == null) {
			
			//TO GET THE CONFIG VERSION
			try {
				this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			//UPDATE
			if(this.config.getInt("ConfigVersion") < configVersion) {
				this.getLogger().info("Updating Config!");
				try {
					ConfigUpdater.update(this.getResourceAsStream("config.yml"), configFile, new ArrayList<>());
					this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		return this.config;
	}
	
	public String translateHexColorCodes(String message){
		
        final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color) + "");
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

	private File getConfigFile() {
		return this.file;
	}

	private void loadConfigCache() {
		this.configCache = new ConfigCacheHandler();
		
		//OPTIONS
		Configuration OptionSection = this.manageFile().getSection("Options");
		this.getAndAddOptions("Options", OptionSection);
		
		//ENABLED FEATURES
		this.manageFile().getSection("FeaturesEnabled").getKeys().forEach(featuresPath -> {
			FeatureType feature = FeatureType.valueOf(featuresPath);
			boolean isEnabled = this.manageFile().getBoolean("FeaturesEnabled." + featuresPath);
			this.configCache.setFeatureEnabled(feature, isEnabled);
		});
		
		//PERMISSIONS
		this.manageFile().getSection("Permissions").getKeys().forEach(permissionPath -> {
			String permission = this.manageFile().getString("Permissions." + permissionPath);
			this.configCache.addPermission("Permissions." + permissionPath, permission);
		});

		//CHANNEL IDS
		this.manageFile().getSection("Channel").getKeys().forEach(channelPath -> {
			FeatureType feature = FeatureType.valueOf(channelPath);
			long channelId = this.manageFile().getLong("Channel." + channelPath);
			this.configCache.setChannelId(feature, channelId);
		});

		//MINECRAFT MESSAGES
		Configuration minecraftMessageSection = this.manageFile().getSection("Messages");
		this.getAndAddMinecraftMessage("Messages", minecraftMessageSection);

		//DISCORD MESSAGES
		Configuration discordMessageSection = this.manageFile().getSection("DiscordMessages");
		this.getAndAddDiscordMessage("DiscordMessages", discordMessageSection);
		
		//DISCORD EMBED MESSAGES
		Configuration discordEmbedSection = this.manageFile().getSection("DiscordEmbedMessages");
		this.getAndAddDiscordEmbed("DiscordEmbedMessages", discordEmbedSection);
	}
	
	//BETTER IDEA? https://www.spigotmc.org/threads/configuration-getselection-with-getkeys-true.520184/#post-4237413
	private void getAndAddOptions(String path, Configuration section) {
		section.getKeys().forEach(optionPath -> {
			Object object = section.get(optionPath);

			if(object instanceof Configuration)
				this.getAndAddOptions(path + "." + optionPath, section.getSection(optionPath));
			else if(object instanceof List<?>)
				this.getConfigCache().addList(path + "." + optionPath, section.getStringList(optionPath));
			else
				this.getConfigCache().addOption(path + "." + optionPath, object.toString());
		});	
	}
	
	private void getAndAddDiscordEmbed(String path, Configuration section) {
		section.getKeys().forEach(optionPath -> {
			Object object = section.get(optionPath);

			if(object instanceof Configuration)
				this.getAndAddDiscordEmbed(path + "." + optionPath, section.getSection(optionPath));
			else if(object instanceof List<?>)
				this.getConfigCache().addList(path + "." + optionPath, section.getStringList(optionPath));
			else
				this.getConfigCache().addEmbedString(path + "." + optionPath, object.toString());
		});	
	}

	private void getAndAddMinecraftMessage(String path, Configuration section) {
		section.getKeys().forEach(optionPath -> {
			Object object = section.get(optionPath);
			
			if(object instanceof Configuration)
				this.getAndAddMinecraftMessage(path + "." + optionPath, section.getSection(optionPath));
			else
				this.getConfigCache().addMinecraftMessage(path + "." + optionPath, this.translateHexColorCodes(section.getString(optionPath)));
		});	
	}
	
	private void getAndAddDiscordMessage(String path, Configuration section) {
		section.getKeys().forEach(optionPath -> {
			Object object = section.get(optionPath);
			
			if(object instanceof Configuration)
				this.getAndAddDiscordMessage(path + "." + optionPath, section.getSection(optionPath));
			else
				this.getConfigCache().addDiscordMessage(path + "." + optionPath, section.getString(optionPath));
		});	
	}
		
	//GET METHODES
	public AsyncMySQL getAsyncMySql() {
		return this.sql;
	}
	
	public OfflineInformationsSQL getOfflineInformationsSQL() {
		return this.offlineInfoSQL;
	}

	public DiscordManager getDiscordManager() {
		return this.discordMGR;
	}

	public VerifySQL getVerifySQL() {
		return this.verifySQL;
	}
	
	public OfflineInformationManager getOfflineInformationManager() {
		return this.offlineInformationManager;
	}
	
	public PluginInformations getPluginInformations() {
		return this.pluginInformations;
	}

	public VerifyManager getVerifyManager() {
		return this.verifyManager;
	}

	public DelayManager getDelayManger() {
		return this.delayManger;
	}

	public ConfigCacheHandler getConfigCache() {
		return this.configCache;
	}

	public PluginMessagingBungeecordManager getMessagingManager() {
		return this.messagingManager;
	}

	public PermissionsAPI getPermissionsAPI() {
		return this.permsAPI;
	}
}
