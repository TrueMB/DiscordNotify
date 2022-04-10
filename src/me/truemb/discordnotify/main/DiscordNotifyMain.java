package me.truemb.discordnotify.main;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import me.truemb.discordnotify.database.AsyncMySQL;
import me.truemb.discordnotify.database.OfflineInformationsSQL;
import me.truemb.discordnotify.database.VerifySQL;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.listener.DiscordNotifyListener;
import me.truemb.discordnotify.manager.ConfigManager;
import me.truemb.discordnotify.manager.DelayManager;
import me.truemb.discordnotify.manager.OfflineInformationManager;
import me.truemb.discordnotify.manager.VerifyManager;
import me.truemb.discordnotify.messaging.PluginMessenger;
import me.truemb.discordnotify.runnable.DN_DiscordBotConnector;
import me.truemb.discordnotify.runnable.DN_InactivityChecker;
import me.truemb.discordnotify.utils.DiscordManager;
import me.truemb.discordnotify.utils.PermissionsAPI;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.player.UniversalPlayer;
import me.truemb.universal.server.UniversalServer;

@Getter
public class DiscordNotifyMain {
	
	private final File dataDirectory;
	private ScheduledExecutorService executor;

	//DATA
	public HashMap<UUID, Long> joinTime = new HashMap<UUID, Long>();
	
	//COMMANDS
    private HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
    private HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
	
	//MySQL
	private AsyncMySQL asyncMySql;
	private OfflineInformationsSQL offlineInformationsSQL;
	private VerifySQL verifySQL;
	
	//MANAGER
	private ConfigManager configManager;
	private DiscordManager discordManager;
	
    private OfflineInformationManager offlineInformationManager;
    private DelayManager delayManager;
    private VerifyManager verifyManager;
    
    //RUNNABLE
    private DN_InactivityChecker inactivityChecker;
    
    //PLUGIN MESSAGING
    private PluginMessenger pluginMessenger;
    
    //API
    private PermissionsAPI permsAPI;

    //UNIVERSAL
	private UniversalServer universalServer;
	private DiscordNotifyListener listener;

	//OWN CONSTRUCTER FOR VELOCITY SINCE THE PROXYSERVER INSTANCE IS NEED ON START
	public DiscordNotifyMain(File dataDirectory, com.velocitypowered.api.proxy.ProxyServer proxy) {
		this.dataDirectory = dataDirectory;
		int cores = Runtime.getRuntime().availableProcessors();
		int usedCores = 2;
		if(usedCores > cores) usedCores = cores;
		
		this.executor = Executors.newScheduledThreadPool(usedCores);
		this.universalServer = UniversalServer.buildServer(ServerType.VELOCITY);
		
		this.getUniversalServer().getVelocityServer().setInstance(proxy);
		
		this.onStart();
	}
	
	public DiscordNotifyMain(File dataDirectory, ServerType type) {
		this.dataDirectory = dataDirectory;
		int cores = Runtime.getRuntime().availableProcessors();
		int usedCores = 2;
		if(usedCores > cores) usedCores = cores;
		
		this.executor = Executors.newScheduledThreadPool(usedCores);
		this.universalServer = UniversalServer.buildServer(type);
		
		this.onStart();
	}
	
	//TODO SPONGE SUPPORT
	//TODO Kick Event
	//TODO Server Start/Stop

	//Velocity needs Spicord v4.2.1
	/**
	 * Enables the DiscordNotify Plugin
	 */
	private void onStart() {

        InputStream configInputStream = getClass().getClassLoader().getResourceAsStream("config.yml");
		this.configManager = new ConfigManager(this.getUniversalServer().getLogger(), configInputStream, this.getDataDirectory());
		
		this.pluginMessenger = new PluginMessenger(this);
		this.listener = new DiscordNotifyListener(this, this.getDiscordChatEnabled());

		//MANAGER
		this.delayManager = new DelayManager();
		this.verifyManager = new VerifyManager(this);
		this.offlineInformationManager = new OfflineInformationManager();
		
		//PERMISSIONS API
		this.permsAPI = new PermissionsAPI(this);
		
		//MYSQL
		this.startMySql();
		
		//DISCORD
		if(!this.getUniversalServer().isProxySubServer()) {
			this.discordManager = new DiscordManager(this);
			this.discordManager.registerAddons(this.getConfigManager().getConfig().getString("Options.DiscordBot.Name"));
			
			new DN_DiscordBotConnector(this); //TASK WHICH CONNECTS THE DISCORD BOT
		}

		//If Server gets reloaded, it sets the current time again
		this.getUniversalServer().getOnlinePlayers().forEach(all -> this.getJoinTime().put(all.getUUID(), System.currentTimeMillis()));
		
		//RUNNABLE
		if(!this.getUniversalServer().isProxySubServer() && this.getConfigManager().isFeatureEnabled(FeatureType.Inactivity))
			this.inactivityChecker = new DN_InactivityChecker(this);	
	}
	
	public void onDisable() {
		
		for(UniversalPlayer players : this.getUniversalServer().getOnlinePlayers()) {
			UUID uuid = players.getUUID();
			
			if(this.getJoinTime().get(uuid) != null) {
				long time = System.currentTimeMillis() - this.getJoinTime().get(uuid);
				
				this.getOfflineInformationsSQL().addToInformation(uuid, InformationType.Playtime, time);
				this.getOfflineInformationManager().addInformation(uuid, InformationType.Playtime, time);
				this.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Playtime, this.getOfflineInformationManager().getInformationLong(uuid, InformationType.Playtime));
				
				this.getJoinTime().remove(uuid);
			}
		}
		
		if(this.inactivityChecker != null)
			this.inactivityChecker.cancelTask();
		
		if(this.getDiscordManager() != null)
			this.getDiscordManager().disconnectDiscordBot();

		if(this.getAsyncMySql() != null && this.getAsyncMySql().getMySQL() != null && this.getAsyncMySql().getMySQL().getConnection() != null)
			this.getAsyncMySql().getMySQL().closeConnection();
	}
	

	//MySQL
	private void startMySql() {
		this.getUniversalServer().getLogger().info("{MySQL}  starting MySQL . . .");
		
		try {
			this.asyncMySql = new AsyncMySQL(this.getUniversalServer().getLogger(), this.getConfigManager());
			this.offlineInformationsSQL = new OfflineInformationsSQL(this.getAsyncMySql(), this.getOfflineInformationManager());
			this.verifySQL = new VerifySQL(this);
			
			this.getExecutor().schedule(new Runnable() {
				
				@Override
				public void run() {
					getOfflineInformationsSQL().setup();
					getVerifySQL().setupVerifications();
				}
			}, 2, TimeUnit.SECONDS);
			
			this.getUniversalServer().getLogger().info("{MySQL}  successfully connected to Database.");
		} catch (Exception e) {
			this.getUniversalServer().getLogger().warning("{MySQL}  Failed to start MySql (" + e.getMessage() + ")");
		}
	}
}
