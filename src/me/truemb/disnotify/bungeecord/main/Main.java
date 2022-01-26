package me.truemb.disnotify.bungeecord.main;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.spicord.bot.DiscordBot.BotStatus;

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
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin{
	
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
	
	//BUNGEECORD MAIN CLASS

	@Override
	public void onEnable() {
		
	    HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
	    HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
	    
		String serverVersion = this.getProxy().getVersion();
		
		//FILES
		this.configManager = new ConfigManager(this.getLogger(), this.getResourceAsStream("config.yml"), this.getDataFolder()); //LOADS CONFIG
		
		//Bungeecord Messaging Channel
		this.messagingManager = new PluginMessagingBungeecordManager(this, this.getConfigManager());
		
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
		this.discordMGR = new DiscordManager(this.getConfigManager(), this.permsAPI, this.getPluginInformations(), this.getOfflineInformationManager(), this.getVerifyManager(), this.getVerifySQL(), this.getDelayManger(), staffChatDisabled, discordChatEnabled);
		this.discordMGR.registerAddons(this.getConfigManager().getConfig().getString("Options.DiscordBot.Name"));

		int id = ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
			
			@Override
			public void run() {
				
				if(discordMGR.getDiscordBot() == null)
					return;
				
				discordMGR.prepareDiscordBot();
				
				if(discordMGR.isDiscordBotHooked())
					ProxyServer.getInstance().getScheduler().cancel(discordMGR.getHookSchedulerId());
				else if(discordMGR.getDiscordBot().getStatus() == BotStatus.OFFLINE) {
					ProxyServer.getInstance().getScheduler().cancel(discordMGR.getHookSchedulerId());
					getLogger().warning("Couldnt connect to the Bot. Is the Bot Offline?");
				}
				
			}
		}, 1, 1, TimeUnit.SECONDS).getId();
		discordMGR.setHookSchedulerId(id);
		
		//LISTENER
		if(this.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave)) {
			BC_JoinLeaveListener joinQuitListener = new BC_JoinLeaveListener(this.getDiscordManager(), this.getConfigManager());
			this.getProxy().getPluginManager().registerListener(this, joinQuitListener);
		}

		if(this.getConfigManager().isFeatureEnabled(FeatureType.Chat)) {
			BC_ChatListener chatListener = new BC_ChatListener(this.getDiscordManager(), this.getConfigManager(), this.getPermissionsAPI(), discordChatEnabled);
			this.getProxy().getPluginManager().registerListener(this, chatListener);
		}
		
		//GENERAL JOIN QUIT FOR MULTIPLE STUFF
		BC_JoinQuitGeneralListener joinQuitListener = new BC_JoinQuitGeneralListener(this.getDiscordManager(), this.getConfigManager(), this.getVerifyManager(), this.getPermissionsAPI(), this.getMessagingManager(), this.getOfflineInformationsSQL(), this.getVerifySQL(), this.joinTime);
		this.getProxy().getPluginManager().registerListener(this, joinQuitListener);
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
		
		BC_VerifyCommand verifyCmd = new BC_VerifyCommand(this.getDiscordManager(), this.getConfigManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getPermissionsAPI());
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
		
		try {
			this.sql = new AsyncMySQL(this.getLogger(), this.getConfigManager());
			this.offlineInfoSQL = new OfflineInformationsSQL(this.getAsyncMySql(), this.getOfflineInformationManager());
			this.verifySQL = new VerifySQL(this.getAsyncMySql(), this.getVerifyManager(), this.getConfigManager(), this.getPluginInformations(), this.getPermissionsAPI(), this.getMessagingManager());
			
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
}
