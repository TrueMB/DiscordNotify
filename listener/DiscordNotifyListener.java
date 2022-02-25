package me.truemb.discordnotify.listener;

import java.util.HashMap;
import java.util.UUID;

import _me.truemb.universal.listener.UniversalEventhandler;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.utils.DisnotifyTools;
import net.dv8tion.jda.api.entities.Member;

public class DiscordNotifyListener extends UniversalEventhandler{
	
	private DiscordNotifyMain instance;
	
	private HashMap<UUID, Boolean> discordChatEnabled;
	
	public DiscordNotifyListener(DiscordNotifyMain plugin, HashMap<UUID, Boolean> discordChatEnabled) {
		this.instance = plugin;
		this.discordChatEnabled = discordChatEnabled;
	}
	
	@Override
	public void onPlayerJoin(UniversalPlayer up, String serverName) {
		
		UUID uuid = up.getUUID();
		
		//IF FEATURE ENABLED
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave))
			this.onPlayerJoinFeature(up, serverName);
		
		//ALWAYS ON JOIN
		this.instance.getJoinTime().put(uuid, System.currentTimeMillis());
		
		//INACTIVITY
		if(this.instance.getOfflineInformationManager().getInformationString(uuid, InformationType.Inactivity) != null 
				&& this.instance.getOfflineInformationsSQL().getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			
			this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getOfflineInformationsSQL().getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Inactivity, "false");
		}
		
		if(this.instance.getVerifyManager().isVerified(uuid) && this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync)) {
		
			if(this.instance.getDiscordManager().getDiscordBot() == null)
				return;
		
			long disuuid = this.instance.getVerifyManager().getVerfiedWith(uuid);

			boolean usePrimaryGroup = this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useOnlyPrimaryGroup");
			String[] currentGroupList;
			
			if(this.instance.getPermsAPI().usePluginBridge) {
				if(usePrimaryGroup)
					this.instance.getPluginMessenger().askForPrimaryGroup(uuid);
				else
					this.instance.getPluginMessenger().askForGroups(uuid);
			}else {
				
				if(usePrimaryGroup)
					currentGroupList = new String[]{ this.instance.getPermsAPI().getPrimaryGroup(uuid) };
				else
					currentGroupList = this.instance.getPermsAPI().getGroups(uuid);
				
				
				Member member = this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
				if(member == null) {
					this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).queue(mem -> {
						DisnotifyTools.checkForRolesUpdate(this.instance, uuid, mem, currentGroupList);
					});
				}else
					DisnotifyTools.checkForRolesUpdate(this.instance, uuid, member, currentGroupList);
			}

		}
		
	}

	
	/**
	 * Will only trigger, if the JoinLeaveFeature is Active
	 */
	private void onPlayerJoinFeature(UniversalPlayer up, String serverName) {

		if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join")))
			return;
		
		UUID uuid = up.getUUID();
		long channelId;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave"))
			channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + serverName);
		else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerJoinLeave);

		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", serverName);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerJoinEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerJoinMessage", placeholder);
		}
		
	}

	@Override
	public void onPlayerQuit(UniversalPlayer up, String serverName) {
		
		UUID uuid = up.getUUID();
		
		//IF FEATURE ENABLED
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave))
			this.onPlayerQuitFeature(up, serverName);
		
		if(this.instance.getJoinTime().get(uuid) != null) {
			long time = System.currentTimeMillis() - this.instance.getJoinTime().get(uuid);
			
			this.instance.getOfflineInformationsSQL().addToInformation(uuid, InformationType.Playtime, time);
			this.instance.getOfflineInformationManager().addInformation(uuid, InformationType.Playtime, time);
			this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Playtime, this.instance.getOfflineInformationManager().getInformationLong(uuid, InformationType.Playtime));
			
			this.instance.getJoinTime().remove(uuid);
		}
		
		//VERIFICATION
		this.instance.getVerifyManager().clearVerficationProgress(uuid);
		
		this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Bungee_Server, serverName);
		this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.Bungee_Server, serverName);
		this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Bungee_Server, serverName);
		
		long lastConnection = System.currentTimeMillis();
		this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.LastConnection, lastConnection);
		this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.LastConnection, lastConnection);
		this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.LastConnection, lastConnection);
		
	}
	
	/**
	 * Will only trigger, if the JoinLeaveFeature is Active
	 */
	private void onPlayerQuitFeature(UniversalPlayer up, String serverName) {

		if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Leave")))
			return;

		UUID uuid = up.getUUID();
		long channelId;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave"))
			channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + serverName);
		else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerJoinLeave);
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", serverName);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerLeaveEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerLeaveMessage", placeholder);
		}
	}


	@Override
	public void onPlayerMessage(UniversalPlayer up, String message) {
		//IF FEATURE ENABLED
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat))
			this.onPlayerMessageFeature(up, message);
		
	}

	private void onPlayerMessageFeature(UniversalPlayer up, String message) {
		UUID uuid = up.getUUID();
		
		if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Chat")))
			return;
		
		//Check if extra Chat is enabled for ChatSyncing
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD MESSAGE
		String server = up.getServer();
		String group = this.instance.getPermsAPI().getPrimaryGroup(uuid);
		
		long channelId;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat"))
			channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + server);
		else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.Chat);
			
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("group", group == null ? "" : group);
		placeholder.put("server", server);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.Chat)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
	
	@Override
	public void onPlayerDeath(UniversalPlayer up, String deathMessage) {
		
		UUID uuid = up.getUUID();
		String ingameName = up.getIngameName();

		if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Death")))
			return;
		
		//DISCORD DEATH MESSAGE
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerDeath);
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", ingameName);
		placeholder.put("UUID", uuid.toString());
		placeholder.put("DeathMessage", deathMessage);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerDeath)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "DeathEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerDeathMessage", placeholder);
		}
	}

}
