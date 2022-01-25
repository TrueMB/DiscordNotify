package me.truemb.disnotify.spigot.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingSpigotManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.Member;

public class MC_JoinLeaveListener implements Listener{

	private DiscordManager discordManager;
	private PluginInformations pluginInfo;
	private VerifyManager verifyManager;
	private ConfigManager configManager;
	private PluginMessagingSpigotManager messagingManager;
	private OfflineInformationsSQL offlineInfoSQL;
	private VerifySQL verifySQL;
	private PermissionsAPI permsAPI;

	public MC_JoinLeaveListener(DiscordManager discordManager, PluginInformations pluginInfo, VerifyManager verifyManager, VerifySQL verifySQL, ConfigManager configManager, PluginMessagingSpigotManager messagingManager, OfflineInformationsSQL offlineInfoSQL, PermissionsAPI permsAPI) {
		this.discordManager = discordManager;
		this.pluginInfo = pluginInfo;
		this.verifyManager = verifyManager;
		this.configManager = configManager;
		this.messagingManager = messagingManager;
		this.offlineInfoSQL = offlineInfoSQL;
		this.verifySQL = verifySQL;
		this.permsAPI = permsAPI;
		
	}
		
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		String ipAddress = p.getAddress().getAddress().getHostAddress();
		this.offlineInfoSQL.updateInformation(uuid, InformationType.IP, ipAddress);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.IP, ipAddress);
		this.messagingManager.sendInformationUpdate(p, InformationType.IP, ipAddress);
		
		//IF BUNGEECORD SUB SERVER, THEN STOP HERE. IF ONLY SPIGOT, THEN SEND MESSAGES
		if(this.pluginInfo.isBungeeCordSubServer())
			return;
		
		//INACTIVITY
		if(this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity) != null && this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			this.offlineInfoSQL.updateInformation(uuid, InformationType.Inactivity, "false");
			this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.messagingManager.sendInformationUpdate(p, InformationType.Inactivity, "false");
		}

		//DISCORD BOT IS NOT READY
		if(this.discordManager.getDiscordBot() == null || this.discordManager.getDiscordBot().getJda() == null || this.discordManager.getDiscordBot().getJda().getGuilds().size() <= 0)
			return;
		
		//DISCORD JOIN MESSAGE
		if(this.configManager.isFeatureEnabled(FeatureType.PlayerJoinLeave)) {

			if(!p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Join"))) {
			
				long channelId = this.configManager.getChannelID(FeatureType.PlayerJoinLeave);
				
				HashMap<String, String> placeholder = new HashMap<>();
				placeholder.put("Player", p.getName());
				placeholder.put("uuid", uuid.toString());
				
				if(this.configManager.useEmbedMessage(FeatureType.PlayerJoinLeave)) {
					this.discordManager.sendEmbedMessage(channelId, uuid, "PlayerJoinEmbed", placeholder);
				}else {
					this.discordManager.sendDiscordMessage(channelId, "PlayerJoinMessage", placeholder);
				}
			}
		}
		
		if(this.verifyManager.isVerified(uuid) && this.configManager.isFeatureEnabled(FeatureType.RoleSync)) {
		
			if(this.discordManager.getDiscordBot() == null)
				return;
		
			long disuuid = this.verifyManager.getVerfiedWith(uuid);
			
			String[] currentGroupList;
	
			if(this.configManager.getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useOnlyPrimaryGroup"))
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

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		Location loc = p.getLocation();

		String ipAddress = p.getAddress().getAddress().getHostAddress();
		this.offlineInfoSQL.updateInformation(uuid, InformationType.IP, ipAddress);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.IP, ipAddress);
		this.messagingManager.sendInformationUpdate(p, InformationType.IP, ipAddress);
		
		String location = this.configManager.getConfig().getString("Options.OtherFormats.Location")
				.replaceAll("(?i)%" + "world" + "%", loc.getWorld().getName())
				.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
				.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
				.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
				.replaceAll("(?i)%" + "yaw" + "%", String.valueOf(Math.round(loc.getYaw() * 100D) / 100D))
				.replaceAll("(?i)%" + "pitch" + "%", String.valueOf(Math.round(loc.getPitch() * 100D) / 100D));
		
		this.offlineInfoSQL.updateInformation(uuid, InformationType.Location, location);
		this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Location, location);
		this.messagingManager.sendInformationUpdate(p, InformationType.Location, location);

		
		//IF BUNGEECORD SUB SERVER, THEN STOP HERE. IF ONLY SPIGOT, THEN SEND MESSAGES
		if(this.pluginInfo.isBungeeCordSubServer())
			return;
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", p.getName());
		placeholder.put("uuid", uuid.toString());
		
		//DISCORD LEAVE MESSAGE
		if(this.configManager.isFeatureEnabled(FeatureType.PlayerJoinLeave)) {

			if(!p.hasPermission(this.configManager.getConfig().getString("Permissions.Bypass.Leave"))) {
				long channelId = this.configManager.getChannelID(FeatureType.PlayerJoinLeave);
				
				if(this.configManager.useEmbedMessage(FeatureType.PlayerJoinLeave)) {
					this.discordManager.sendEmbedMessage(channelId, uuid, "PlayerLeaveEmbed", placeholder);
				}else {
					this.discordManager.sendDiscordMessage(channelId, "PlayerLeaveMessage", placeholder);
				}
			}
		}
		
		//VERIFICATION
		this.verifyManager.clearVerficationProgress(uuid);
	}
}
