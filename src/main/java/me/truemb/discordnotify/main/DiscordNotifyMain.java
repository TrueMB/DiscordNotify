package me.truemb.discordnotify.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bukkit.OfflinePlayer;

import lombok.Getter;
import me.truemb.discordnotify.database.OfflineInformationsSQL;
import me.truemb.discordnotify.database.VerifySQL;
import me.truemb.discordnotify.database.connector.AsyncMySQL;
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
import me.truemb.discordnotify.runnable.DN_SubServerPinger;
import me.truemb.discordnotify.utils.DiscordManager;
import me.truemb.discordnotify.utils.PermissionsAPI;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.player.UniversalPlayer;
import me.truemb.universal.server.UniversalServer;

@Getter
public class DiscordNotifyMain {
	
    public static final int BSTATS_PLUGIN_ID = 12029;
		
	private final File dataDirectory;
	private PluginDescription pluginDescription;
	private ScheduledExecutorService executor;

	//DATA
	public HashMap<UUID, Long> joinTime = new HashMap<UUID, Long>();
	
	//COMMANDS
    private HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
    private HashMap<UUID, Boolean> staffChatToggle = new HashMap<>();
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
    private DN_SubServerPinger subServerHandler;
    
    //PLUGIN MESSAGING
    private PluginMessenger pluginMessenger;
    
    //API
    private PermissionsAPI permsAPI;

    //UNIVERSAL
	private UniversalServer universalServer;
	private DiscordNotifyListener listener;

	//OWN CONSTRUCTER FOR VELOCITY SINCE THE PROXYSERVER INSTANCE IS NEED ON START
	public DiscordNotifyMain(File dataDirectory, com.velocitypowered.api.proxy.ProxyServer proxy, PluginDescription pluginDescription) {
		this.dataDirectory = dataDirectory;
		this.pluginDescription = pluginDescription;
		
		int cores = Runtime.getRuntime().availableProcessors();
		int usedCores = 2;
		if(usedCores > cores) usedCores = cores;
		
		this.executor = Executors.newScheduledThreadPool(usedCores);
		this.universalServer = UniversalServer.buildServer(ServerType.VELOCITY);
		
		this.getUniversalServer().getVelocityServer().setInstance(proxy);
		
		this.onStart();
	}
	
	public DiscordNotifyMain(File dataDirectory, ServerType type, PluginDescription pluginDescription) {
		this.dataDirectory = dataDirectory;
		this.pluginDescription = pluginDescription;
		
		int cores = Runtime.getRuntime().availableProcessors();
		int usedCores = 2;
		if(usedCores > cores) usedCores = cores;
		
		this.executor = Executors.newScheduledThreadPool(usedCores);
		this.universalServer = UniversalServer.buildServer(type);
		
		this.onStart();
	}
	
	//TODO SPONGE SUPPORT - Version 9 must be Supported from Spicord first

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
		this.verifyManager = new VerifyManager();
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
		
		//SCANS THE SERVER ONCE FOR PLAYTIME OF THE SERVER, TO UPDATE THE DATABASE
		this.scanOfflinePlayersForData();
		//If Server gets reloaded, it sets the current time again
		this.getUniversalServer().getOnlinePlayers().forEach(all -> this.getJoinTime().put(all.getUUID(), System.currentTimeMillis()));
		
		//RUNNABLE
		if(!this.getUniversalServer().isProxySubServer() && this.getConfigManager().isFeatureEnabled(FeatureType.Inactivity))
			this.inactivityChecker = new DN_InactivityChecker(this);
		
		if(this.getUniversalServer().isProxy() && this.getConfigManager().isFeatureEnabled(FeatureType.ServerStatus))
			this.subServerHandler = new DN_SubServerPinger(this);	
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

	    if(this.subServerHandler != null)
	    	this.subServerHandler.cancelTask();
		
		if(this.getDiscordManager() != null)
			this.getDiscordManager().disconnectDiscordBot();

		if(this.getAsyncMySql() != null && this.getAsyncMySql().getMySQL() != null && this.getAsyncMySql().getMySQL().getConnection() != null)
			this.getAsyncMySql().getMySQL().closeConnection();
	}
	
	private void scanOfflinePlayersForData() {
		
		File file = new File(this.getDataDirectory(), "ScanIsDone");
		
		if(file.exists())
			return;

		if(this.getUniversalServer().getServerPlatform() == ServerType.BUKKIT) {
			this.getUniversalServer().getLogger().info("Scanning Playerdata the first time. This can take some time.");
			
			org.bukkit.OfflinePlayer[] players = org.bukkit.Bukkit.getOfflinePlayers();
			Collection<OfflinePlayer> all = Arrays.asList(players);
			
			new Thread(() -> {
				
				int counter = 0;
				for(OfflinePlayer player : all) {
					counter++;
					UUID uuid = player.getUniqueId();
		
					long lastTimePlayed = player.getLastPlayed();
					long playtimeInMilli = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50; //Normally / 20 ticks * 1000 Millis, but since a long doesn't have a comma, we wont divide
					
					this.getOfflineInformationManager().addInformation(uuid, InformationType.Playtime, playtimeInMilli);
					this.getOfflineInformationsSQL().addToInformation(uuid, InformationType.Playtime, playtimeInMilli);
		
					if(this.getOfflineInformationManager().getInformationLong(uuid, InformationType.LastConnection) < lastTimePlayed) {
						this.getOfflineInformationManager().setInformation(uuid, InformationType.LastConnection, lastTimePlayed); //ONLY NEWEST
						this.getOfflineInformationsSQL().updateInformation(uuid, InformationType.LastConnection, lastTimePlayed);
					}
					if(counter == 20) {
						counter = 0;
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				
				try {
					file.createNewFile();
					this.getUniversalServer().getLogger().info("Sent the playtime of the players to the main server.");
				} catch (IOException e) {
					this.getUniversalServer().getLogger().warning("Couldn't create ScanIsDone File. Please create it manually or look for the Issue. Otherwise the scan will be done on each server start and add the playtime again.");
				}
				
			}).start();
		}
		//TODO SPONGE METHOD NEEDS TO BE ADDED
		
	}
	
	//MySQL
	private void startMySql() {
		this.getUniversalServer().getLogger().info("{SQL} starting SQL . . .");
		
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
			
			this.getUniversalServer().getLogger().info("{SQL} successfully connected to Database.");
		} catch (Exception e) {
			e.printStackTrace();
			this.getUniversalServer().getLogger().warning("{SQL} Failed to start SQL (" + e.getMessage() + ")");
		}
	}
}
