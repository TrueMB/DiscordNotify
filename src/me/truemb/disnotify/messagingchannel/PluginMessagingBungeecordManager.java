package me.truemb.disnotify.messagingchannel;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.truemb.disnotify.bungeecord.main.Main;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessagingBungeecordManager implements Listener {

	private Main instance;
	private String channel;

	// THIS IS THE BUNGEECOR SIDE. BUNGEECORD RECEIVES INFORMATION FROM MINECRAFT
	// SERVER

	public PluginMessagingBungeecordManager(Main plugin) {
		this.channel = "discord:notify";
		this.instance = plugin;

		this.instance.getProxy().registerChannel(this.channel);
		this.instance.getProxy().getPluginManager().registerListener(this.instance, this);
	}

	@EventHandler
	public void onPluginMessageReceived(PluginMessageEvent event) {
		
		if (!event.getTag().equalsIgnoreCase(this.channel))
			return;
		
		ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		String subChannel = in.readUTF();

		// the receiver is a ProxiedPlayer when a server talks to the proxy
		if (event.getReceiver() instanceof ProxiedPlayer) {
			
			ProxiedPlayer receiver = (ProxiedPlayer) event.getReceiver();
			UUID uuid = receiver.getUniqueId();
			
			if (subChannel.equalsIgnoreCase("DEATH")) {
				
				long channelId = this.instance.getConfigCache().getChannelId(FeatureType.PlayerDeath);
				String deathMessage = in.readUTF();

				HashMap<String, String> placeholder = new HashMap<>();
				placeholder.put("DeathMessage", deathMessage);
				
				if(this.instance.getConfigCache().useEmbedMessage(FeatureType.PlayerDeath)) {
					this.instance.getDiscordManager().sendEmbedMessage(channelId, uuid, "DeathEmbed", placeholder);
				}else {
					this.instance.getDiscordManager().sendDiscordMessage(channelId, "PlayerDeathMessage", placeholder);
				}
				
			}else if (subChannel.equalsIgnoreCase("GET_GROUPS_ANSWER")) {
				
				String groupS = in.readUTF();
				String[] currentGroupList = groupS.split(", ");
				
				//ACCEPTING REQUEST
				this.instance.getVerifySQL().acceptVerification(this.instance.getDiscordManager(), uuid, receiver.getName(), currentGroupList);
			
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
	public void askForGroups(ProxiedPlayer player) {
		
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("GET_GROUPS_REQUEST"); // the channel could be whatever you want
	 
	    // we send the data to the server
	    // using ServerInfo the packet is being queued if there are no players in the server
	    // using only the server to send data the packet will be lost if no players are in it
		ServerInfo server = player.getServer().getInfo();
		if(server == null)
			return;
		
		server.sendData(this.channel, out.toByteArray());
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
		
		
		serverInfo.sendData(this.channel, out.toByteArray());
	}
	
	public void sendInformationUpdate(ProxiedPlayer player, InformationType type, long value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(type.toString());
		out.writeLong(value);

		ServerInfo server = player.getServer().getInfo();
		if(server == null)
			return;
		
		server.sendData(this.channel, out.toByteArray());
	}
}
