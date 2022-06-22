package me.truemb.discordnotify.listener;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.listener.UniversalEventhandler;
import me.truemb.universal.player.UniversalPlayer;
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
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave))
			this.onPlayerJoinFeature(up, serverName);
		
		//ALWAYS ON JOIN
		this.instance.getJoinTime().put(uuid, System.currentTimeMillis());
		
		//CHECK FOR NAME CHANGE
		this.instance.getOfflineInformationsSQL().checkForNameChange(uuid, up.getIngameName());
		
		//INACTIVITY
		if(this.instance.getOfflineInformationManager().getInformationString(uuid, InformationType.Inactivity) != null 
				&& this.instance.getOfflineInformationsSQL().getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			
			this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getOfflineInformationsSQL().getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getPluginMessenger().sendInformationUpdate(uuid, serverName, InformationType.Inactivity, "false"); 
		}
		
		//REMINDER - If not verified
		if(this.instance.getDiscordManager() != null && this.instance.getDiscordManager().isAddonEnabled("disnotify::verify") && !this.instance.getVerifyManager().isVerified(uuid))
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.reminder", true));
		
		//ROLE UPDATES
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getVerifyManager().isVerified(uuid) && this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync)) {
		
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
						this.instance.getVerifyManager().checkForRolesUpdate(uuid, mem, currentGroupList);
					});
				}else
					this.instance.getVerifyManager().checkForRolesUpdate(uuid, member, currentGroupList);
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
		
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerJoinEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerJoinMessage", placeholder);
		}
		
	}

	@Override
	public void onPlayerQuit(UniversalPlayer up, String serverName) {
		
		UUID uuid = up.getUUID();
		
		//IF FEATURE ENABLED
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave))
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
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat))
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
		
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().useEmbedMessage(FeatureType.Chat)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "ChatEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, "ChatMessage", placeholder);
		}
	}
	
	@Override
	public void onPlayerDeath(UniversalPlayer up, String deathMessage) {
		if(this.instance.getUniversalServer().isProxySubServer()){
			this.instance.getPluginMessenger().sendPlayerDeath(up.getUUID(), deathMessage);
			return;
		}
		
		UUID uuid = up.getUUID();
		String ingameName = up.getIngameName();

		if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Death")))
			return;
		
		//DISCORD DEATH MESSAGE
		String server = up.getServer();
		long channelId;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerDeath.toString() + ".enableServerSeperatedDeath"))
			channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerDeath.toString() + ".serverSeperatedDeath." + server);
		else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerDeath);
		
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

	@Override
	public void onPlayerServerChange(UniversalPlayer up, String oldServerName, String newServerName) {

		UUID uuid = up.getUUID();
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("old", oldServerName);
		placeholder.put("target", newServerName);
		
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave")) {
			//SEND A MESSAGE TO THE OLD SERVER
			if(!up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Leave"))) {
				for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false)){
					if(server.equalsIgnoreCase(oldServerName)) {
						long channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);
						
						if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
							this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerServerChangeLeaveEmbed", placeholder);
						}else {
							this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerServerChangeLeaveMessage", placeholder);
						}
						break;
					}
				}
			}
			
			//SEND A MESSAGE TO THE NEW SERVER
			if(!up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join"))) {
				for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false)){
					if(server.equalsIgnoreCase(newServerName)) {
						long channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);
						
						if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
							this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerServerChangeJoinEmbed", placeholder);
						}else {
							this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerServerChangeJoinMessage", placeholder);
						}
						break;
					}
				}
			}

		} else {

			if(up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join")))
				return;
			
			long channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerJoinLeave);
			
			if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerJoinLeave)) {
				this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "PlayerServerChangeEmbed", placeholder);
			}else {
				this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerServerChangeMessage", placeholder);
			}
		}
	}

}
