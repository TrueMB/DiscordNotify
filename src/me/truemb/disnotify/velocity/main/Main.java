package me.truemb.disnotify.velocity.main;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.spicord.bot.DiscordBot.BotStatus;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler.TaskBuilder;

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
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.runnable.BC_InactivityChecker;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PluginInformations;

@Plugin(id = "discordnotify", name = "DiscordNotify", version = "1.0", authors = {"TrueMB"})
public class Main{
	
	//VELOCITY
	private ProxyServer proxy;
	private Logger logger;
	
	//MySQL
	private AsyncMySQL sql;
	private OfflineInformationsSQL offlineInfoSQL;
	private VerifySQL verifySQL;
	
	//DISCORD BOT
	private DiscordManager discordMGR;
    
    //MANAGER + HANDLER
	private ConfigManager configManager;
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
	
	//VELOCITY MAIN CLASS

    @Inject
    public Main(ProxyServer server, Logger logger) {
        this.proxy = server;
        this.logger = logger;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent e) {
		
	    HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
	    HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
	    
		String serverVersion = this.proxy.getVersion().getVersion();
		
		//FILES
		this.configManager = new ConfigManager(this.logger, this.getResourceAsStream("config.yml"), this.proxy.get); //LOADS CONFIG
		
		//Bungeecord Messaging Channel
		this.messagingManager = new PluginMessagingBungeecordManager(this);
		
		//MANAGERS
		this.pluginInformations = new PluginInformations(this.proxy.getVersion().getName(), this.getDescription().getVersion(), serverVersion, this.logger, true, false);
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
		this.discordMGR = new DiscordManager(this.getConfigManager(), this.permsAPI, this.getPluginInformations(), this.getOfflineInformationManager(), this.getVerifyManager(), this.getVerifySQL(), this.getDelayManger(), staffChatDisabled, discordChatEnabled);
		this.discordMGR.registerAddons(this.getConfigManager().getConfig().getString("Options.DiscordBot.Name"));

		ScheduledTask task = this.proxy.getScheduler().buildTask(this, new Runnable() {
			
			@Override
			public void run() {
				
				if(discordMGR.getDiscordBot() == null)
					return;
				
				discordMGR.prepareDiscordBot();
				
				if(discordMGR.isDiscordBotHooked())
					System.out.println();
					//ProxyServer.getInstance().getScheduler().cancel(discordMGR.getHookSchedulerId());
				else if(discordMGR.getDiscordBot().getStatus() == BotStatus.OFFLINE) {
					//proxy.getScheduler().cancel(discordMGR.getHookSchedulerId());
					logger.warning("Couldnt connect to the Bot. Is the Bot Offline?");
				}
				
			}
		})
		.repeat(1, TimeUnit.SECONDS).schedule();
		task.getClass()
		discordMGR.setHookSchedulerId(id);
		
		//LISTENER
		if(this.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave)) {
			BC_JoinLeaveListener joinQuitListener = new BC_JoinLeaveListener(this.getDiscordManager(), this.getConfigManager());
			this.getProxy().getPluginManager().registerListener(this, joinQuitListener);
		}

		if(this.getConfigManager().isFeatureEnabled(FeatureType.Chat)) {
			BC_ChatListener chatListener = new BC_ChatListener(this.getDiscordManager(), this.getConfigManager(), discordChatEnabled);
			this.getProxy().getPluginManager().registerListener(this, chatListener);
		}
		
		//GENERAL JOIN QUIT FOR MULTIPLE STUFF
		BC_JoinQuitGeneralListener joinQuitListener = new BC_JoinQuitGeneralListener(this.getVerifyManager(), this.getMessagingManager(), this.getOfflineInformationsSQL(), this.joinTime);
		this.proxy.getPluginManager().registerListener(this, joinQuitListener);
		//================================================================================
		
		//COMMANDS
		if(this.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
			BC_DChatCommand dchatCmd = new BC_DChatCommand(this.getConfigManager(), discordChatEnabled);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, dchatCmd);
		}
		
		if(this.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			BC_StaffCommand staffCmd = new BC_StaffCommand(this.getDiscordManager(), this.getConfigManager(), staffChatDisabled);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, staffCmd);
		}
		
		BC_VerifyCommand verifyCmd = new BC_VerifyCommand(this.getDiscordManager(), this.getConfigManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getMessagingManager(), this.getPermissionsAPI());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, verifyCmd);
				
		//If Server gets reloaded, it sets the current time again
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UUID uuid = all.getUniqueId();
			
			this.joinTime.put(uuid, System.currentTimeMillis());
		}
		
		//RUNNABLES
		if(this.getConfigManager().isFeatureEnabled(FeatureType.Inactivity))
			this.inactivityCheckerRunn = ProxyServer.getInstance().getScheduler().schedule(this, new BC_InactivityChecker(this.getDiscordManager(), this.getPluginInformations(), this.getConfigManager(), this.getAsyncMySql(), this.getOfflineInformationsSQL()), 15, 60 * this.getConfigManager().getConfig().getInt("Options.Inactivity.CheckTimer"), TimeUnit.SECONDS).getId();
		
	}
	
	public void onDisable() {
		
		//If Server gets reloaded or stoped, data gets saved
		for(Player all : this.proxy.getAllPlayers()) {
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
		logger.info("{MySQL}  starting MySQL . . .");
		
		try {
			this.sql = new AsyncMySQL(this.getLogger(), this.getConfigManager());
			this.offlineInfoSQL = new OfflineInformationsSQL(this.getAsyncMySql(), this.getOfflineInformationManager());
			this.verifySQL = new VerifySQL(this.getAsyncMySql(), this.getVerifyManager(), this.getConfigManager(), this.getPluginInformations(), this.getPermissionsAPI());
			
			ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
				
				@Override
				public void run() {
					getOfflineInformationsSQL().setup();
					getVerifySQL().setupVerifications();
				}
			}, 2, TimeUnit.SECONDS);
			
			this.logger.info("{MySQL}  successfully connected to Database.");
		} catch (Exception e) {
			this.logger.warning("{MySQL}  Failed to start MySql (" + e.getMessage() + ")");
			this.isPluginEnabled = false;
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

	public PluginMessagingBungeecordManager getMessagingManager() {
		return this.messagingManager;
	}

	public PermissionsAPI getPermissionsAPI() {
		return this.permsAPI;
	}

	public ProxyServer getProxy() {
		return this.proxy;
	}
}
