package me.truemb.disnotify.messagingchannel;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.truemb.disnotify.bungeecord.main.Main;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import net.dv8tion.jda.api.entities.Member;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessagingBungeecordManager implements Listener {

	private Main instance;
	private static String channel;
	
	private ConfigManager configManager;

	// THIS IS THE BUNGEECOR SIDE. BUNGEECORD RECEIVES INFORMATION FROM MINECRAFT
	// SERVER

	public PluginMessagingBungeecordManager(Main plugin, ConfigManager configManager) {
		PluginMessagingBungeecordManager.channel = "discord:notify";
		this.instance = plugin;
		this.configManager = configManager;

		this.instance.getProxy().registerChannel(PluginMessagingBungeecordManager.channel);
		this.instance.getProxy().getPluginManager().registerListener(this.instance, this);
	}

	@EventHandler
	public void onPluginMessageReceived(PluginMessageEvent event) {
		
		if (!event.getTag().equalsIgnoreCase(PluginMessagingBungeecordManager.channel))
			return;
		
		ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		String subChannel = in.readUTF();

		// the receiver is a ProxiedPlayer when a server talks to the proxy
		if (event.getReceiver() instanceof ProxiedPlayer) {
			
			ProxiedPlayer receiver = (ProxiedPlayer) event.getReceiver();
			UUID uuid = receiver.getUniqueId();
			
			if (subChannel.equalsIgnoreCase("DEATH")) {

				if(this.configManager.getConfig().getBoolean("Options.EnableBypassPermission") && receiver.hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.Bypass.Death")))
					return;

				String server = receiver.getServer().getInfo().getName();
				long channelId;
				if(this.configManager.getConfig().getBoolean("Options." + FeatureType.PlayerDeath.toString() + ".enableServerSeperatedDeath"))
					channelId = this.configManager.getConfig().getLong("Options." + FeatureType.PlayerDeath.toString() + ".serverSeperatedDeath." + server);
				else
					channelId = this.configManager.getChannelID(FeatureType.PlayerDeath);
				
				String deathMessage = in.readUTF();

				HashMap<String, String> placeholder = new HashMap<>();
				placeholder.put("DeathMessage", deathMessage);
				placeholder.put("server", server);
				
				if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerDeath)) {
					this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "DeathEmbed", placeholder);
				}else {
					this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerDeathMessage", placeholder);
				}
				
			}else if (subChannel.equalsIgnoreCase("GET_GROUPS_ANSWER")) {
				
				String groupS = in.readUTF();
				String[] currentGroupList = groupS.split(", ");
				
				long disuuid = this.instance.getVerifyManager().getVerfiedWith(uuid);
				
				//ACCEPTING REQUEST
				Member member = this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
				if(member == null) {
					this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).queue(mem -> {
		
						DisnotifyTools.checkForRolesUpdate(uuid, mem, this.configManager, this.instance.getVerifyManager(), this.instance.getVerifySQL(), this.instance.getDiscordManager(), currentGroupList);
						
					});
				}else
					DisnotifyTools.checkForRolesUpdate(uuid, member, this.configManager, this.instance.getVerifyManager(), this.instance.getVerifySQL(), this.instance.getDiscordManager(), currentGroupList);
			
			}else if (subChannel.equalsIgnoreCase("INFO_UPDATE")) {
				
				InformationType type = InformationType.valueOf(in.readUTF());
				
				if(type.equals(InformationType.LastConnection) || type.equals(InformationType.Playtime)) {
					long value = in.readLong();
					this.instance.getOfflineInformationManager().setInformation(uuid, type, value);
				}else {
					String value = in.readUTF();
					this.instance.getOfflineInformationManager().setInformation(uuid, type, value);
				}
			}else if (subChannel.equalsIgnoreCase("SERVER_STATUS")) {
				//ONLY SENDS MESSAGE, IF PLAYER ON SERVER. MAKES NO SENSE FOR SERVER START AND STOP
				
				//String server = in.readUTF();
				//boolean status = in.readBoolean();
				
			}
			
		}
	}

	//ASKS THE SERVER OF THE USER CONNECTION, WHAT GROUPS HE GOTS
	public void askForGroups(UUID uuid) {
	    ServerInfo server = ProxyServer.getInstance().getPlayer(uuid).getServer().getInfo();
		this.askForGroups(server, uuid);
	}
	
	//ASKS THE SERVER OF THE USER CONNECTION, WHAT PRIMARY GROUP HE GOTS
	public void askForPrimaryGroup(UUID uuid) {
	    ServerInfo server = ProxyServer.getInstance().getPlayer(uuid).getServer().getInfo();
		this.askForPrimaryGroup(server, uuid);
	}
	
	//ASKS THE SERVER OF THE USER CONNECTION, WHAT GROUPS HE GOTS
	public void askForGroups(ServerInfo server, UUID uuid) {
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GET_GROUPS_REQUEST"); // the channel could be whatever you want
	    out.writeUTF(uuid.toString()); // UUID of the target
		
		server.sendData(channel, out.toByteArray());
	}
	
	//ASKS THE SERVER OF THE USER CONNECTION, WHAT PRIMARY GROUP HE GOTS
	public void askForPrimaryGroup(ServerInfo server, UUID uuid) {
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GET_PRIMARYGROUP_REQUEST"); // the channel could be whatever you want
	    out.writeUTF(uuid.toString()); // UUID of the target
	    
		server.sendData(channel, out.toByteArray());
	}
	
	public static void sendGroupAction(ProxiedPlayer player, GroupAction action, String[] groups) {
		sendGroupAction(player.getServer().getInfo(), player.getUniqueId(), action, groups);
	}
	
	public static void sendGroupAction(ServerInfo server, UUID uuid, GroupAction action, String[] groups) {
	    
	    String groupS = "";
		for(String group : groups) {
			groupS += ", " + group;
		}
		groupS = groupS.substring(2, groupS.length());
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GROUP_ACTION"); // the channel could be whatever you want
		out.writeUTF(uuid.toString());
		
	    out.writeUTF(action.toString()); // the channel could be whatever you want
		out.writeUTF(groupS);
		
		server.sendData(channel, out.toByteArray());
	}

	public void sendInformationUpdate(ProxiedPlayer player, InformationType type, String value) {
		this.sendInformationUpdate(player.getServer().getInfo(), player.getUniqueId(), type, value);
	}

	public void sendInformationUpdate(ServerInfo server, UUID uuid, InformationType type, String value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(uuid.toString());
		out.writeUTF(type.toString());
		out.writeUTF(value);

		server.sendData(channel, out.toByteArray());
	}

	public void sendInformationUpdate(ProxiedPlayer player, InformationType type, long value) {
		this.sendInformationUpdate(player.getServer().getInfo(), player.getUniqueId(), type, value);
	}

	public void sendInformationUpdate(ServerInfo server, UUID uuid, InformationType type, long value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(uuid.toString());
		out.writeUTF(type.toString());
		out.writeLong(value);

		server.sendData(channel, out.toByteArray());
	}
}
