package me.truemb.discordnotify.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.spicord.bot.DiscordBot;

import club.minnced.discord.webhook.WebhookClient;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.enums.MinotarTypes;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.listener.UniversalEventhandler;
import me.truemb.universal.player.UniversalLocation;
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
		
		//DO NOTHING - Since Management Server does it already
		if(this.instance.getUniversalServer().isProxySubServer())
			return;
		
		//IF FEATURE ENABLED
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerJoinLeave))
			this.onPlayerJoinFeature(up, serverName);
		
		//ALWAYS ON JOIN
		this.instance.getJoinTime().put(uuid, System.currentTimeMillis());
		
		if(this.instance.getDiscordManager().getStaticEmbedManager() != null)
			this.instance.getDiscordManager().getStaticEmbedManager().updateAllEmbeds();
		
		//CHECK FOR NAME CHANGE
		this.instance.getOfflineInformationsSQL().checkForNameChange(uuid, up.getIngameName());
			
		// ========[ OFFLINE DATA ]=======
			
		//INACTIVITY
		if(this.instance.getOfflineInformationManager().getInformationString(uuid, InformationType.Inactivity) != null 
				&& this.instance.getOfflineInformationsSQL().getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true")) {
			
			this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getOfflineInformationsSQL().getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "false");
			this.instance.getPluginMessenger().sendInformationUpdate(uuid, serverName, InformationType.Inactivity, "false"); 
		}
	
		//IP
		String ipAddress = up.getIP();
		
		this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.IP, ipAddress);
		this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.IP, ipAddress);
		this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.IP, ipAddress);
		
		// ========[ OFFLINE DATA ]=======

		//REMINDER - If not verified
		if(this.instance.getDiscordManager() != null && this.instance.getDiscordManager().isAddonEnabled("disnotify::verify") && !this.instance.getVerifyManager().isVerified(uuid))
			up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.reminder", true));
		
		//ROLE UPDATES
		if(this.instance.getVerifyManager().isVerified(uuid) && this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync)) {
		
			DiscordBot discordBot = this.instance.getDiscordManager().getDiscordBot();
			if(discordBot == null)
				return;
		
			long disuuid = this.instance.getVerifyManager().getVerfiedWith(uuid);

			boolean syncDiscordToMinecraft = this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft");
			
			if(syncDiscordToMinecraft) {
				
				List<String> groups = new ArrayList<>();
				Member member = this.instance.getDiscordManager().getCurrentGuild().getMemberById(disuuid);
				if(member == null) {
					this.instance.getDiscordManager().getCurrentGuild().retrieveMemberById(disuuid).queue(mem -> {
						this.instance.getDiscordManager().syncRoles(uuid, mem, null);
					});
				}else {
					this.instance.getDiscordManager().syncRoles(uuid, member, null);
				}
				
				//TODO BUNGEECORD PLUGIN MESSAGING CHANNEL?
				groups.forEach(group -> this.instance.getPermsAPI().addGroup(uuid, group));
				
			}else {
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
					
					
					Member member = this.instance.getDiscordManager().getCurrentGuild().getMemberById(disuuid);
					if(member == null) {
						this.instance.getDiscordManager().getCurrentGuild().retrieveMemberById(disuuid).queue(mem -> {
							this.instance.getDiscordManager().syncRoles(uuid, mem, currentGroupList);
						});
					}else
						this.instance.getDiscordManager().syncRoles(uuid, member, currentGroupList);
				}
			}
		}
		
	}

	
	/**
	 * Will only trigger, if the JoinLeaveFeature is Active
	 */
	private void onPlayerJoinFeature(UniversalPlayer up, String serverName) {
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join")))
			return;

		UUID uuid = up.getUUID();
		String channelId = null;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false))
				if(servers.equalsIgnoreCase(serverName))
					channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + servers);
		}else
			channelId = this.instance.getConfigManager().getChannel(FeatureType.PlayerJoinLeave);

		//Server should not send Messages
		if(channelId == null || channelId.equals("") || channelId.equals("-1"))
			return;

		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", serverName);
		
		switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerJoinLeave)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerJoinMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "PlayerJoinEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {
				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerJoinLeave, serverName, channelId);
				
				String minotarTypeS = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Join.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Join.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
		
	}
	
	@Override
	public void onPlayerQuit(UniversalPlayer up, String serverName) {
		
		UUID uuid = up.getUUID();

		//LOCATION - Needs to be sent from the Bukkit Servers, even if it is a Network.
		if(!this.instance.getUniversalServer().isProxy()) {
			UniversalLocation loc = up.getLocation();
			String location = this.instance.getConfigManager().getConfig().getString("Options.OtherFormats.Location")
					.replaceAll("(?i)%" + "world" + "%", loc.getWorldname())
					.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
					.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
					.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
					.replaceAll("(?i)%" + "yaw" + "%", String.valueOf(Math.round(loc.getYaw() * 100D) / 100D))
					.replaceAll("(?i)%" + "pitch" + "%", String.valueOf(Math.round(loc.getPitch() * 100D) / 100D));
			
			this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Location, location);
			this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.Location, location);
			this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Location, location);
		}
		
		//DO NOTHING - Since Management Server does it already
		if(this.instance.getUniversalServer().isProxySubServer())
			return;
		
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
		
		if(this.instance.getDiscordManager().getStaticEmbedManager() != null)
			this.instance.getDiscordManager().getStaticEmbedManager().updateAllEmbeds();
		
		//VERIFICATION
		this.instance.getVerifyManager().clearVerficationProgress(uuid);

		// ========[ OFFLINE DATA - START]=======
		
		//LAST CONNECTED SERVER
		this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Bungee_Server, serverName);
		this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.Bungee_Server, serverName);
		this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.Bungee_Server, serverName);

		//LAST CONNECTION
		long lastConnection = System.currentTimeMillis();
		this.instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.LastConnection, lastConnection);
		this.instance.getOfflineInformationManager().setInformation(uuid, InformationType.LastConnection, lastConnection);
		this.instance.getPluginMessenger().sendInformationUpdate(uuid, InformationType.LastConnection, lastConnection);
		
		// ========[ OFFLINE DATA - END]=======
		
	}
	
	/**
	 * Will only trigger, if the JoinLeaveFeature is Active
	 */
	private void onPlayerQuitFeature(UniversalPlayer up, String serverName) {

		if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Leave")))
			return;

		UUID uuid = up.getUUID();
		String channelId = null;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false))
				if(servers.equalsIgnoreCase(serverName))
					channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + servers);
		}else
			channelId = this.instance.getConfigManager().getChannel(FeatureType.PlayerJoinLeave);
		
		//Server should not send Messages
		if(channelId == null || channelId.equals("") || channelId.equals("-1"))
			return;
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", serverName);
		
		
		switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerJoinLeave)) {	
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerLeaveMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "PlayerLeaveEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {

				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerJoinLeave, serverName, channelId);
				
				String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Leave.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Leave.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
	}


	@Override
	public boolean onPlayerMessage(UniversalPlayer up, String message) {
		
		//IF FEATURE ENABLED
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff))
			if(this.onPlayerStaffMessageFeature(up, message))
				return true;
		
		if(!this.instance.getUniversalServer().isProxySubServer() && this.instance.getConfigManager().isFeatureEnabled(FeatureType.Chat))
			this.onPlayerMessageFeature(up, message);
		
		return false;
	}
	
	private boolean onPlayerStaffMessageFeature(UniversalPlayer up, String message) {
		UUID uuid = up.getUUID();
		
		if(!this.instance.getStaffChatToggle().containsKey(uuid) || !this.instance.getStaffChatToggle().get(uuid))
			return false;
		
		//Staff Message
		
		//ALL PLAYERS INGAME
		for(UniversalPlayer all : this.instance.getUniversalServer().getOnlinePlayers()) {
			UUID uuidAll = all.getUUID();
			if(all.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"))) {
				if(up.equals(all) || !this.instance.getStaffChatDisabled().containsKey(uuidAll) || !this.instance.getStaffChatDisabled().get(uuidAll)) {
					all.sendMessage(this.instance.getConfigManager().getMinecraftMessage("minecraftStaffMessage", true)
							.replaceAll("(?i)%" + "message" + "%", message)
							.replaceAll("(?i)%" + "player" + "%", up.getIngameName())
							.replaceAll("(?i)%" + "server" + "%", up.getServer() != null ? up.getServer() : ""));
				}
			}
		}
				
		//DISCORD STAFF MESSAGE
		String channelId = this.instance.getConfigManager().getChannel(FeatureType.Staff);
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("Message", message);
		placeholder.put("UUID", uuid.toString());
		placeholder.put("server", up.getServer() != null ? up.getServer() : "");
				
		switch (this.instance.getConfigManager().getMessageType(FeatureType.Staff)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "StaffMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Staff.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "StaffEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Staff.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {
				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.Staff, channelId);
				String minotarTypeS = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Staff.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Staff.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
		
		return true; //Cancel Event
		
	}

	private void onPlayerMessageFeature(UniversalPlayer up, String message) {
		UUID uuid = up.getUUID();
		
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Chat")))
			return;
		
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".onlyVerified") && !this.instance.getVerifyManager().isVerified(up.getUUID()))
			return;
		
		//Check if extra Chat is enabled for ChatSyncing
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableSplittedChat"))
			if(!this.discordChatEnabled.containsKey(uuid) || !this.discordChatEnabled.get(uuid))
				return;
		
		//DISCORD MESSAGE
		String server = up.getServer();
		String group = this.instance.getPermsAPI().getPrimaryGroup(uuid);
		
		String channelId = null;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Chat.toString() + ".enableServerSeperatedChat")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat").getKeys(false))
				if(servers.equalsIgnoreCase(server))
					channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Chat.toString() + ".serverSeperatedChat." + servers);
		}else
			channelId = this.instance.getConfigManager().getChannel(FeatureType.Chat);

		//Server should not send Messages
		if(channelId == null || channelId.equals("") || channelId.equals("-1"))
			return;
			
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Message", message);
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("group", group == null ? "" : group);
		placeholder.put("server", server);
		
		if(this.instance.getUniversalServer().isProxySubServer())
			return;

		switch (this.instance.getConfigManager().getMessageType(FeatureType.Chat)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "ChatMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Chat.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "ChatEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.Chat.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {

				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.Chat, server, channelId);
				
				String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Chat.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Chat.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
	}
	
	@Override
	public void onPlayerDeath(UniversalPlayer up, String deathMessage) {

		//IF FEATURE ENABLED
		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerDeath))
			return;
		
		if(this.instance.getUniversalServer().isProxySubServer()){
			this.instance.getPluginMessenger().sendPlayerDeath(up.getUUID(), deathMessage);
			return;
		}
		
		UUID uuid = up.getUUID();
		String ingameName = up.getIngameName();

		if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Death")))
			return;
		
		//DISCORD DEATH MESSAGE
		String server = up.getServer();
		String channelId = null;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerDeath.toString() + ".enableServerSeperatedDeath")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerDeath.toString() + ".serverSeperatedDeath").getKeys(false))
				if(servers.equalsIgnoreCase(server))
					channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerDeath.toString() + ".serverSeperatedDeath." + servers);
		}else
			channelId = this.instance.getConfigManager().getChannel(FeatureType.PlayerDeath);

		//Server should not send Messages
		if(channelId == null || channelId.equals("") || channelId.equals("-1"))
			return;
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", ingameName);
		placeholder.put("UUID", uuid.toString());
		placeholder.put("DeathMessage", deathMessage);
		placeholder.put("server", up.getServer() != null ? up.getServer() : "");
		
		switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerDeath)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerDeathMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerDeath.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "DeathEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerDeath.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {
	
				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerDeath, server, channelId);
				
				String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Death.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Death.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
	}

	@Override
	public void onPlayerServerChange(UniversalPlayer up, String oldServerName, String newServerName) {
		this.instance.getDiscordManager().getStaticEmbedManager().updateAllEmbeds();
		
		if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enablePlayerServerSwitch"))
			return;

		UUID uuid = up.getUUID();
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", up.getIngameName());
		placeholder.put("UUID", uuid.toString());
		placeholder.put("old", oldServerName);
		placeholder.put("target", newServerName);
		
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerJoinLeave.toString() + ".enableServerSeperatedJoinLeave")) {
			//SEND A MESSAGE TO THE OLD SERVER
			if(!this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") || !up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Leave"))) {
				for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false)){
					if(server.equalsIgnoreCase(oldServerName)) {
						String channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);

						if(channelId != null && !channelId.equals("") && !channelId.equals("-1")){
						
							switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerJoinLeave)) {
								case MESSAGE: {
									try {
										this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerServerChangeLeaveMessage", placeholder);
									}catch (NumberFormatException ex) {
										this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
									}
									break;
								}
								case EMBED: {
									try {
										this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "PlayerServerChangeLeaveEmbed", placeholder);
									}catch (NumberFormatException ex) {
										this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
									}
									break;
								}
								case WEBHOOK: {
						
									WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerJoinLeave, server, channelId);
									
									String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChangeLeave.PictureType");
									MinotarTypes minotarType = MinotarTypes.BUST;
									try {
										minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
									}catch(Exception ex) { /* NOTING */ }
									
									String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChangeLeave.Description");
									this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
									break;
								}
							}
							break;
						}
					}
				}
			}
			
			//SEND A MESSAGE TO THE NEW SERVER
			if(!this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") || !up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join"))) {
				for(String server : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave").getKeys(false)){
					if(server.equalsIgnoreCase(newServerName)) {
						String channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerJoinLeave.toString() + ".serverSeperatedJoinLeave." + server);

						if(channelId != null && !channelId.equals("") && !channelId.equals("-1")){
							
							switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerJoinLeave)) {
								case MESSAGE: {
									try {
										this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerServerChangeJoinMessage", placeholder);
									}catch (NumberFormatException ex) {
										this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
									}
									break;
								}
								case EMBED: {
									try {
										this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "PlayerServerChangeJoinEmbed", placeholder);
									}catch (NumberFormatException ex) {
										this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
									}
									break;
								}
								case WEBHOOK: {
						
									WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerJoinLeave, server, channelId);
									
									String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChangeJoin.PictureType");
									MinotarTypes minotarType = MinotarTypes.BUST;
									try {
										minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
									}catch(Exception ex) { /* NOTING */ }
									
									String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChangeJoin.Description");
									this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
									break;
								}
							}
							break;
						}
					}
				}
			}

		} else {

			if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Join")))
				return;
			
			String channelId = this.instance.getConfigManager().getChannel(FeatureType.PlayerJoinLeave);
			
			switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerJoinLeave)) {
				case MESSAGE: {
					try {
						this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerServerChangeMessage", placeholder);
					}catch (NumberFormatException ex) {
						this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
					}
					break;
				}
				case EMBED: {
					try {
						this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "PlayerServerChangeEmbed", placeholder);
					}catch (NumberFormatException ex) {
						this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerJoinLeave.toString() + " couldn't parse the Channel ID.");
					}
					break;
				}
				case WEBHOOK: {
		
					WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerJoinLeave, channelId);
					
					String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChange.PictureType");
					MinotarTypes minotarType = MinotarTypes.BUST;
					try {
						minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
					}catch(Exception ex) { /* NOTING */ }
					
					String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.PlayerServerChange.Description");
					this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
					break;
				}
			}
		}
	}

	@Override
	public void onPlayerAdvancement(UniversalPlayer up, String advancementKey) {

		//IF FEATURE ENABLED
		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.PlayerAdvancement))
			return;
		
		if(this.instance.getUniversalServer().isProxySubServer()){
			this.instance.getPluginMessenger().sendPlayerAdvancement(up.getUUID(), advancementKey);
			return;
		}
		
		UUID uuid = up.getUUID();
		String ingameName = up.getIngameName();

		if(this.instance.getConfigManager().getConfig().getBoolean("Options.activateBypassPermission") && up.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Advancement")))
			return;
		
		//DISCORD DEATH MESSAGE
		String server = up.getServer();
		String channelId = null;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.PlayerAdvancement.toString() + ".enableServerSeperatedAdvancement")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.PlayerAdvancement.toString() + ".serverSeperatedAdvancement").getKeys(false))
				if(servers.equalsIgnoreCase(server))
					channelId = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.PlayerAdvancement.toString() + ".serverSeperatedAdvancement." + servers);
		}else
			channelId = this.instance.getConfigManager().getChannel(FeatureType.PlayerAdvancement);

		//Server should not send Messages
		if(channelId == null || channelId.equals("") || channelId.equals("-1"))
			return;
		
		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("Player", ingameName);
		placeholder.put("UUID", uuid.toString());
		placeholder.put("AdvancementName", advancementKey);
		placeholder.put("server", up.getServer() != null ? up.getServer() : "");
		
		switch (this.instance.getConfigManager().getMessageType(FeatureType.PlayerAdvancement)) {
			case MESSAGE: {
				try {
					this.instance.getDiscordManager().sendDiscordMessage(Long.parseLong(channelId), "PlayerAdvancementMessage", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerAdvancement.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case EMBED: {
				try {
					this.instance.getDiscordManager().sendEmbedMessage(Long.parseLong(channelId), uuid, "AdvancementEmbed", placeholder);
				}catch (NumberFormatException ex) {
					this.instance.getUniversalServer().getLogger().warning("The Feature: " + FeatureType.PlayerAdvancement.toString() + " couldn't parse the Channel ID.");
				}
				break;
			}
			case WEBHOOK: {
	
				WebhookClient webhookClient = this.instance.getDiscordManager().createOrLoadWebhook(FeatureType.PlayerAdvancement, server, channelId);
				
				String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Advancement.PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				String description = this.instance.getConfigManager().getConfig().getString("DiscordWebhookMessages.Advancement.Description");
				this.instance.getDiscordManager().sendWebhookMessage(webhookClient, up.getIngameName(), "https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString(), description, placeholder);
				break;
			}
		}
	}

}
