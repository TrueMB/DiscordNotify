package me.truemb.disnotify.spigot.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.spicord.bot.DiscordBot.BotStatus;

import de.jeff_media.updatechecker.UpdateChecker;
import de.jeff_media.updatechecker.UserAgentBuilder;
import me.truemb.disnotify.database.AsyncMySQL;
import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingSpigotManager;
import me.truemb.disnotify.runnable.MC_InactivityChecker;
import me.truemb.disnotify.spigot.commands.MC_DChatCommand;
import me.truemb.disnotify.spigot.commands.MC_StaffCommand;
import me.truemb.disnotify.spigot.commands.MC_VerifyCommand;
import me.truemb.disnotify.spigot.listener.MC_ChatListener;
import me.truemb.disnotify.spigot.listener.MC_DeathListener;
import me.truemb.disnotify.spigot.listener.MC_JoinLeaveListener;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PluginInformations;

public class Main extends JavaPlugin{
	
	//MySQL
	private AsyncMySQL sql;
	private OfflineInformationsSQL offlineInfoSQL;
	private VerifySQL verifySQL;
	
	//DISCORD BOT
	private DiscordManager discordMGR;
	
	private PermissionsAPI permsAPI;
	
    private static final int SPIGOT_RESOURCE_ID = 94230;
    private static final int BSTATS_PLUGIN_ID = 12029;
    
    //CACHE
    public HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
    
    //MANAGER + HANDLER
    private PluginInformations pluginInformations;
    private OfflineInformationManager offlineInformationManager;
    private DelayManager delayManger;
    private VerifyManager verifyManager;
    private PluginMessagingSpigotManager messagingManager;
	private ConfigManager configManager;
	
	private int inactivityCheckerRunn = -1;
	public boolean placeholderSupport = false;
			
	//SPIGOT MAIN CLASS
	
	@Override
	public void onEnable() {
		
	    HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
	    HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();

		String serverVersion = Bukkit.getServer().getVersion();
		org.bukkit.configuration.file.YamlConfiguration spigotCfg = Bukkit.spigot().getConfig();
		boolean isBungeeCordSubServer = spigotCfg.getBoolean("settings.bungeecord");
		
		//FILES
		this.configManager = new ConfigManager(this.getLogger(), this.getResource("config.yml"), this.getDataFolder()); //LOADS CONFIG
		
		//Bungeecord Messaging Channel
		this.messagingManager = new PluginMessagingSpigotManager(this);
		
		//MANAGERS
		this.pluginInformations = new PluginInformations(this.getDescription().getName(), this.getDescription().getVersion(), serverVersion, this.getLogger(), false, isBungeeCordSubServer);
		this.delayManger = new DelayManager();
		this.verifyManager = new VerifyManager(this.getDelayManger());
		this.offlineInformationManager = new OfflineInformationManager();
		
		//PERMISSIONS API
		this.permsAPI = new PermissionsAPI(this.getPluginInformations());
		
		//MANAGER

		//MySQL
		this.startMySql();
		
		if(!Bukkit.getPluginManager().isPluginEnabled(this))
			return; //DATABASE MISSING
		
		this.placeholderSupport = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
		
		//SPICORD
		if(!isBungeeCordSubServer) {
			this.discordMGR = new DiscordManager(this.getConfigManager(), this.permsAPI, this.getPluginInformations(), this.getOfflineInformationManager(), this.getVerifyManager(), this.getVerifySQL(), this.getDelayManger(), staffChatDisabled, discordChatEnabled);

			String botname = this.getConfigManager().getConfig().getString("Options.DiscordBot.Name");
			this.discordMGR.registerAddons(botname);
			
			int id = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
				
				@Override
				public void run() {
					
					if(discordMGR.getDiscordBot() == null)
						return;
					
					discordMGR.prepareDiscordBot();
					
					if(discordMGR.isDiscordBotHooked())
						Bukkit.getScheduler().cancelTask(discordMGR.getHookSchedulerId());
					else if(discordMGR.getDiscordBot().getStatus() == BotStatus.OFFLINE) {
						Bukkit.getScheduler().cancelTask(discordMGR.getHookSchedulerId());
						getLogger().warning("Couldnt connect to the Bot. Is the Bot Offline?");
					}
					
				}
			}, 20, 20).getTaskId();
			discordMGR.setHookSchedulerId(id);
		}
		
		//LISTENER
		//BUNGEECORD GETS MANAGED IN THE CLASS
		MC_JoinLeaveListener joinQuitListener = new MC_JoinLeaveListener(this.getDiscordManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getConfigManager(), this.getMessagingManager(), this.getOfflineInformationsSQL(), this.getPermissionsAPI());
		this.getServer().getPluginManager().registerEvents(joinQuitListener, this);

		//SEND PLUGINMESSAGE, IF BUNGEECORD
		if(this.getConfigManager().isFeatureEnabled(FeatureType.PlayerDeath)) {
			MC_DeathListener deathListener = new MC_DeathListener(this);
			this.getServer().getPluginManager().registerEvents(deathListener, this);
		}
		
		//NO NEED FOR THIS CLASS, IF BUNGEECORD
		if(!isBungeeCordSubServer) {
			if(this.getConfigManager().isFeatureEnabled(FeatureType.Chat)) {
				MC_ChatListener chatListener = new MC_ChatListener(this.getDiscordManager(), this.getConfigManager(), discordChatEnabled);
				this.getServer().getPluginManager().registerEvents(chatListener, this);
			}
		}
		
		//================================================================================
		
		//COMMANDS
		
		//Enable only if Spicord runs on the SubServer, otherwise is the command already enabled on the bungeecord Proxy
		if(!isBungeeCordSubServer) {

			try{
			    Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			    commandMapField.setAccessible(true);
			    CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

				
				MC_StaffCommand staffCommand = new MC_StaffCommand(this.getDiscordManager(), this.getConfigManager(), staffChatDisabled);
				commandMap.register("staff", staffCommand);
				List<String> staffAliases = new ArrayList<>();
				staffAliases.add("s");
				staffCommand.setAliases(staffAliases);
				
				MC_VerifyCommand verifyCommand = new MC_VerifyCommand(this.getDiscordManager(), this.getConfigManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getPermissionsAPI());
				commandMap.register("verify", verifyCommand);
				

				if(this.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
					MC_DChatCommand dchatCmd = new MC_DChatCommand(this.getConfigManager(), discordChatEnabled);
					commandMap.register("dchat", dchatCmd);
				}
			    
			}catch(Exception exception){
			    exception.printStackTrace();
			}
		}
		
		
		
		//METRICS ANALYTICS
		if(this.getConfigManager().getConfig().getBoolean("Options.useMetrics"))
			new Metrics(this, BSTATS_PLUGIN_ID);
		
		//CHECK FOR UPDATE
		this.checkForUpdate();
		
		
		//RUNNABLES
		if(!isBungeeCordSubServer)
			if(this.getConfigManager().isFeatureEnabled(FeatureType.Inactivity))
				this.inactivityCheckerRunn = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new MC_InactivityChecker(this.getDiscordManager(), this.getPluginInformations(), this.getConfigManager(), this.getOfflineInformationsSQL()), 20 * 15, 20 * 60 * this.getConfigManager().getConfig().getInt("Options.Inactivity.CheckTimer")).getTaskId();
		
	}
	
	@Override
	public void onDisable() {

		if(this.getPluginInformations().isBungeeCordSubServer()) {
			//TODO ServerStatus needs to work even with no players.
		}
		
		for(Player all : Bukkit.getOnlinePlayers()) {
			
			UUID uuid = all.getUniqueId();
			Location loc = all.getLocation();
			
			String location = loc.getWorld().getName() + ", x=" + loc.getBlockX() + ", y=" + loc.getBlockY() + ", z=" + loc.getBlockZ() + ", yaw=" + loc.getYaw() + ", pitch=" + loc.getPitch();
			this.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Location, location);
		}
		
		Bukkit.getScheduler().cancelTask(this.inactivityCheckerRunn);
		if(this.getDiscordManager() != null)
			this.getDiscordManager().disconnectDiscordBot();
	}
	
	//THIS METHODE ONLY RUNS ONCE AND AFTER DATABASE CONNECTION
	//NEEDS TO SEND UPDATES TO SQL FOR THE FUTURE AND BUNGEECORD AND UPDATE OFFLINE DATA
	private void scanOfflinePlayersForData() {
		
		File file = new File(this.getDataFolder(), "ScanIsDone");
		
		if(file.exists())
			return;

		for(OfflinePlayer player : Bukkit.getOfflinePlayers()){
			UUID uuid = player.getUniqueId();

			long lastTimePlayed = player.getLastPlayed();
			long playtimeInTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
			
			this.getOfflineInformationManager().addInformation(uuid, InformationType.Playtime, playtimeInTicks);
			this.getOfflineInformationsSQL().addToInformation(uuid, InformationType.Playtime, playtimeInTicks);

			if(this.getOfflineInformationManager().getInformationLong(uuid, InformationType.LastConnection) < lastTimePlayed) {
				this.getOfflineInformationManager().setInformation(uuid, InformationType.LastConnection, lastTimePlayed); //ONLY NEWEST
				this.getOfflineInformationsSQL().updateInformation(uuid, InformationType.LastConnection, lastTimePlayed);
			}
		}
		
		try {
			file.createNewFile();
		} catch (IOException e) {
			this.getLogger().warning("Couldn't create ScanIsDone File. Please create it manually or look for the Issue. Otherwise the scan will be done on each server start and add the playtime again.");
		}
	}
	
	//CHECK FOR UPDATE
	//https://www.spigotmc.org/threads/powerful-update-checker-with-only-one-line-of-code.500010/
	private void checkForUpdate() {
        UpdateChecker.init(this, SPIGOT_RESOURCE_ID) // A link to a URL that contains the latest version as String
                .setDownloadLink(SPIGOT_RESOURCE_ID) // You can either use a custom URL or the Spigot Resource ID
                .setDonationLink("https://www.paypal.me/truemb")
                .setChangelogLink(SPIGOT_RESOURCE_ID) // Same as for the Download link: URL or Spigot Resource ID
                .setNotifyOpsOnJoin(true) // Notify OPs on Join when a new version is found (default)
                .setNotifyByPermissionOnJoin(this.getDescription().getName() + ".updatechecker") // Also notify people on join with this permission
                .setUserAgent(new UserAgentBuilder().addPluginNameAndVersion())
                .checkEveryXHours(12) // Check every hours
                .checkNow(); // And check right now
        
	}
	
	//MySQL
	private void startMySql() {
		this.getLogger().info("{MySQL}  starting MySQL . . .");
		
		Main plugin = this;
		
		try {
			this.sql = new AsyncMySQL(this.getLogger(), this.getConfigManager());
			this.offlineInfoSQL = new OfflineInformationsSQL(this.getAsyncMySql(), this.getOfflineInformationManager());
			this.verifySQL = new VerifySQL(this.getAsyncMySql(), this.getVerifyManager(), this.getConfigManager(), this.getPluginInformations(), this.getPermissionsAPI(), null);
			
			Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
				
				@Override
				public void run() {
					getOfflineInformationsSQL().setup();
					getVerifySQL().setupVerifications();
					
					if(getPluginInformations().isBungeeCordSubServer()) {
						
						Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
							
							@Override
							public void run() {
								scanOfflinePlayersForData();
							}
						}, 60);
					}
					
				}
			}, 20);
			
			this.getLogger().info("{MySQL} Successfully connected to Database.");
		} catch (Exception e) {
			this.getLogger().warning("{MySQL}  Failed to start MySql (" + e.getMessage() + ")");
			Bukkit.getPluginManager().disablePlugin(this); //DISABLE PLUGIN, SINCE IT NEEDS THE DATABASE
		}
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

	public PermissionsAPI getPermissionsAPI() {
		return this.permsAPI;
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

	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	public PluginMessagingSpigotManager getMessagingManager() {
		return messagingManager;
	}

}
