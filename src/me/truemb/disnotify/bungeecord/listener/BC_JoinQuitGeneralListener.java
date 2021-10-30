package me.truemb.disnotify.bungeecord.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BC_JoinQuitGeneralListener implements Listener{

	private VerifyManager verifyManager;
	private PluginMessagingBungeecordManager messagingManager;
	private OfflineInformationsSQL offlineInfoSQL;
	
	private HashMap<UUID, Long> joinTime;

	public BC_JoinQuitGeneralListener(VerifyManager verifyManager, PluginMessagingBungeecordManager messagingManager, OfflineInformationsSQL offlineInfoSQL, HashMap<UUID, Long> joinTime) {
		this.verifyManager = verifyManager;
		this.messagingManager = messagingManager;
		this.offlineInfoSQL = offlineInfoSQL;
		this.joinTime = joinTime;
	}
	
	@EventHandler
	public void onJoin(ServerConnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		this.joinTime.put(uuid, System.currentTimeMillis());
		
		//INACTIVITY
		if(this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity) != null && this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			this.offlineInfoSQL.updateInformation(uuid, InformationType.Inactivity, "false");
			this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.messagingManager.sendInformationUpdate(p, InformationType.Inactivity, "false");
		}
	}

	@EventHandler
	public void onQuit(ServerDisconnectEvent e) {
		ProxiedPlayer p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		if(this.joinTime.get(uuid) != null) {
			long time = System.currentTimeMillis() - this.joinTime.get(uuid);
			
			this.offlineInfoSQL.addToInformation(uuid, InformationType.Playtime, time);
			this.offlineInfoSQL.getOfflineInfoManager().addInformation(uuid, InformationType.Playtime, time);
			this.messagingManager.sendInformationUpdate(p, InformationType.Playtime, this.offlineInfoSQL.getOfflineInfoManager().getInformationLong(uuid, InformationType.Playtime));
			
			this.joinTime.remove(uuid);
		}
		
		//VERIFICATION
		this.verifyManager.clearVerficationProgress(uuid);
		
		String server = e.getTarget().getName();
		this.offlineInfoSQL.updateInformation(uuid, InformationType.Bungee_Server, server);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Bungee_Server, server);
		this.messagingManager.sendInformationUpdate(p, InformationType.Bungee_Server, server);
		
		long lastConnection = System.currentTimeMillis();
		this.offlineInfoSQL.updateInformation(uuid, InformationType.LastConnection, lastConnection);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.LastConnection, lastConnection);
		this.messagingManager.sendInformationUpdate(p, InformationType.LastConnection, lastConnection);
	}
}
