package me.truemb.disnotify.messagingchannel;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.velocity.main.Main;

public class PluginMessagingVelocityManager{

	private Main instance;
	private static String channel;

	// THIS IS THE BUNGEECOR SIDE. BUNGEECORD RECEIVES INFORMATION FROM MINECRAFT
	// SERVER

	public PluginMessagingVelocityManager(Main plugin) {
		PluginMessagingVelocityManager.channel = "discord:notify";
		this.instance = plugin;

		this.instance.getProxy().registerChannel(PluginMessagingVelocityManager.channel);
		this.instance.getProxy().getEventManager().register(this.instance, this);
	}

	@Subscribe
	public void onPluginMessageReceived(PluginMessageEvent event) {
		
		if (!event.getIdentifier().getId().equalsIgnoreCase(PluginMessagingVelocityManager.channel))
			return;
		
		ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		String subChannel = in.readUTF();

		// the receiver is a ProxiedPlayer when a server talks to the proxy
		if (event.getTarget() instanceof Player) {
			
			Player receiver = (Player) event.getTarget();
			UUID uuid = receiver.getUniqueId();
			
			if (subChannel.equalsIgnoreCase("DEATH")) {
				
				long channelId = this.instance.getConfigManager().getChannelID(FeatureType.PlayerDeath);
				String deathMessage = in.readUTF();

				HashMap<String, String> placeholder = new HashMap<>();
				placeholder.put("DeathMessage", deathMessage);
				
				if(this.instance.getConfigManager().useEmbedMessage(FeatureType.PlayerDeath)) {
					this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "DeathEmbed", placeholder);
				}else {
					this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerDeathMessage", placeholder);
				}
				
			}else if (subChannel.equalsIgnoreCase("GET_GROUPS_ANSWER")) {
				
				String groupS = in.readUTF();
				String[] currentGroupList = groupS.split(", ");
				
				//ACCEPTING REQUEST
				this.instance.getVerifySQL().acceptVerification(this.instance.getDiscordManager(), uuid, receiver.getGameProfile().getName(), currentGroupList);
			
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
	
	
	//ASKS THE SERVER OF THE USER CONNECTION, WHAT GROUPS HE GOT
	public void askForGroups(Player player) {
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GET_GROUPS_REQUEST"); // the channel could be whatever you want
	 
	    // we send the data to the server
	    // using ServerInfo the packet is being queued if there are no players in the server
	    // using only the server to send data the packet will be lost if no players are in it
		ServerInfo server = player.getCurrentServer().get().getServerInfo();
		if(server == null)
			return;
		
		server.sendData(channel, out.toByteArray());
	}
	
	public static void sendGroupAction(ProxiedPlayer player, GroupAction action, String[] groups) {
	    
	    String groupS = "";
		for(String group : groups) {
			groupS += ", " + group;
		}
		groupS = groupS.substring(2, groupS.length());
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GROUP_ACTION"); // the channel could be whatever you want
		out.writeUTF(player.getUniqueId().toString());
		
	    out.writeUTF(action.toString()); // the channel could be whatever you want
		out.writeUTF(groupS);
	 
	    // we send the data to the server
	    // using ServerInfo the packet is being queued if there are no players in the server
	    // using only the server to send data the packet will be lost if no players are in it
		ServerInfo server = player.getServer().getInfo();
		if(server == null)
			return;
		
		server.sendData(channel, out.toByteArray());
	}

	public void sendInformationUpdate(ProxiedPlayer player, InformationType type, String value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(type.toString());
		out.writeUTF(value);

		Server server = player.getServer();
		
		if(server == null)
			return;
		
		ServerInfo serverInfo = server.getInfo();
		
		if(serverInfo == null)
			return;
		
		
		serverInfo.sendData(channel, out.toByteArray());
	}
	
	public void sendInformationUpdate(ProxiedPlayer player, InformationType type, long value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(type.toString());
		out.writeLong(value);

		ServerInfo server = player.getServer().getInfo();
		if(server == null)
			return;
		
		server.sendData(channel, out.toByteArray());
	}
}
