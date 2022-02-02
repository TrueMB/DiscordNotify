package me.truemb.disnotify.bungeecord.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import net.dv8tion.jda.api.entities.Member;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BC_JoinQuitGeneralListener implements Listener{

	private DiscordManager discordManager;
	private ConfigManager configManager;
	private VerifyManager verifyManager;
	private PermissionsAPI permsAPI;
	private PluginMessagingBungeecordManager messagingManager;
	private OfflineInformationsSQL offlineInfoSQL;
	private VerifySQL verifySQL;
	
	private HashMap<UUID, Long> joinTime;

	public BC_JoinQuitGeneralListener(DiscordManager discordManager, ConfigManager configManager, VerifyManager verifyManager, PermissionsAPI permsAPI, PluginMessagingBungeecordManager messagingManager, OfflineInformationsSQL offlineInfoSQL, VerifySQL verifySQL, HashMap<UUID, Long> joinTime) {
		this.discordManager = discordManager;
		this.configManager = configManager;
		this.verifyManager = verifyManager;
		this.permsAPI = permsAPI;
		this.messagingManager = messagingManager;
		this.offlineInfoSQL = offlineInfoSQL;
		this.verifySQL = verifySQL;
		this.joinTime = joinTime;
	}
	
	@EventHandler
	public void onJoin(ServerConnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		ServerInfo server = e.getTarget();
		UUID uuid = p.getUniqueId();
		
		this.joinTime.put(uuid, System.currentTimeMillis());
		
		//INACTIVITY
		if(this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity) != null && this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			this.offlineInfoSQL.updateInformation(uuid, InformationType.Inactivity, "false");
			this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.messagingManager.sendInformationUpdate(server, uuid, InformationType.Inactivity, "false");
		}
		
		if(this.verifyManager.isVerified(uuid) && this.configManager.isFeatureEnabled(FeatureType.RoleSync)) {
		
			if(this.discordManager.getDiscordBot() == null)
				return;
		
			long disuuid = this.verifyManager.getVerfiedWith(uuid);

			boolean usePrimaryGroup = this.configManager.getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useOnlyPrimaryGroup");
			String[] currentGroupList;
			
			if(this.permsAPI.usePluginBridge) {
				if(usePrimaryGroup)
					this.messagingManager.askForPrimaryGroup(e.getTarget(), uuid);
				else
					this.messagingManager.askForGroups(e.getTarget(), uuid);
			}else {
				
				if(usePrimaryGroup)
					currentGroupList = new String[]{ this.permsAPI.getPrimaryGroup(uuid) };
				else
					currentGroupList = this.permsAPI.getGroups(uuid);
				
				
				Member member = this.discordManager.getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
				if(member == null) {
					this.discordManager.getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).queue(mem -> {
						DisnotifyTools.checkForRolesUpdate(uuid, mem, this.configManager, this.verifyManager, this.verifySQL, this.discordManager, currentGroupList);
					});
				}else
					DisnotifyTools.checkForRolesUpdate(uuid, member, this.configManager, this.verifyManager, this.verifySQL, this.discordManager, currentGroupList);
			}

		}
	}

	@EventHandler
	public void onQuit(ServerDisconnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		ServerInfo server = e.getTarget();
		UUID uuid = p.getUniqueId();
		
		if(this.joinTime.get(uuid) != null) {
			long time = System.currentTimeMillis() - this.joinTime.get(uuid);
			
			this.offlineInfoSQL.addToInformation(uuid, InformationType.Playtime, time);
			this.offlineInfoSQL.getOfflineInfoManager().addInformation(uuid, InformationType.Playtime, time);
			this.messagingManager.sendInformationUpdate(server, uuid, InformationType.Playtime, this.offlineInfoSQL.getOfflineInfoManager().getInformationLong(uuid, InformationType.Playtime));
			
			this.joinTime.remove(uuid);
		}
		
		//VERIFICATION
		this.verifyManager.clearVerficationProgress(uuid);
		
		this.offlineInfoSQL.updateInformation(uuid, InformationType.Bungee_Server, server.getName());
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Bungee_Server, server.getName());
		this.messagingManager.sendInformationUpdate(p, InformationType.Bungee_Server, server.getName());
		
		long lastConnection = System.currentTimeMillis();
		this.offlineInfoSQL.updateInformation(uuid, InformationType.LastConnection, lastConnection);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.LastConnection, lastConnection);
		this.messagingManager.sendInformationUpdate(p, InformationType.LastConnection, lastConnection);
	}
}
