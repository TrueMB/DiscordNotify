package me.truemb.discordnotify.main;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.player.UniversalPlayer;
import _me.truemb.universal.server.UniversalServer;
import lombok.Getter;
import me.truemb.discordnotify.listener.DiscordNotifyListener;
import me.truemb.disnotify.database.AsyncMySQL;
import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.runnable.DN_DiscordBotConnector;
import me.truemb.disnotify.runnable.DN_InactivityChecker;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;

@Getter
public class DiscordNotifyMain {
	
	private final File dataDirectory = new File("\\plugins\\DiscordNotify\\");
	private ScheduledExecutorService executor;

	//DATA
	public HashMap<UUID, Long> joinTime = new HashMap<UUID, Long>();
	
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
    private PluginMessagingBungeecordManager messagingManager;
    
    private DN_InactivityChecker inactivityChecker;
    
    //API
    private PermissionsAPI permsAPI;

    //UNIVERSAL
	private UniversalServer universalServer;
	private DiscordNotifyListener listener;
	
	//TODO VELOCITY MAIN NEED TO SET UNIVERSAL PROXY INSTANCE
	//REMEMBER new DiscordNotify needs to be started on the Server instance
	public DiscordNotifyMain(File dataDirectory, ServerType type) {
		int cores = Runtime.getRuntime().availableProcessors();
		int usedCores = 2;
		if(usedCores > cores) usedCores = cores;
		
		this.executor = Executors.newScheduledThreadPool(usedCores);
		this.universalServer = UniversalServer.buildServer(type);
		
		this.onStart();
	}
	
	/**
	 * Enables the DiscordNotify Plugin
	 */
	private void onStart() {
	    HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
	    HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();

        InputStream configInputStream = getClass().getClassLoader().getResourceAsStream("config.yml");
		this.configManager = new ConfigManager(this.getUniversalServer().getLogger(), configInputStream, this.dataDirectory);
		
		this.listener = new DiscordNotifyListener(this);

		//MANAGER
		this.delayManager = new DelayManager();
		this.verifyManager = new VerifyManager(this.getDelayManager());
		this.offlineInformationManager = new OfflineInformationManager();
		
		//PERMISSIONS API
		this.permsAPI = new PermissionsAPI(this);
		
		//MYSQL
		this.startMySql();
		
		this.discordManager = new DiscordManager(this, staffChatDisabled, discordChatEnabled);
		this.discordManager.registerAddons(this.getConfigManager().getConfig().getString("Options.DiscordBot.Name"));
		
		new DN_DiscordBotConnector(this); //TASK WHICH CONNECTS THE DISCORD BOT
		
		//RUNNABLES
		if(this.getConfigManager().isFeatureEnabled(FeatureType.Inactivity))
			this.inactivityChecker = new DN_InactivityChecker(this);	
	}
	
	public void onDisable() {
		
		for(UniversalPlayer players : this.getUniversalServer().getOnlinePlayers()) {
		UUID uuid = players.getUUID();
		
		if(this.joinTime.get(uuid) != null) {
			long time = System.currentTimeMillis() - this.joinTime.get(uuid);
			
			this.getOfflineInformationsSQL().addToInformation(uuid, InformationType.Playtime, time);
			this.getOfflineInformationManager().addInformation(uuid, InformationType.Playtime, time);
			this.messagingManager.sendInformationUpdate(all, InformationType.Playtime, this.getOfflineInformationManager().getInformationLong(uuid, InformationType.Playtime));
			
			this.joinTime.remove(uuid);
		}
	}
		
		if(this.inactivityChecker != null)
			this.inactivityChecker.cancelTask();
		
		if(this.getDiscordManager() != null)
			this.getDiscordManager().disconnectDiscordBot();
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
