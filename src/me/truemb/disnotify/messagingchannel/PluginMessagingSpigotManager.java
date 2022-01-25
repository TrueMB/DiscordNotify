package me.truemb.disnotify.messagingchannel;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.spigot.main.Main;

public class PluginMessagingSpigotManager implements PluginMessageListener {

	private Main instance;
	private String channel;

	// THIS IS THE SPIGOT SIDE AND ONLY SENDS INFORMATION FROM SPIGOT TO BUNGEECORD

	public PluginMessagingSpigotManager(Main plugin) {
		this.instance = plugin;
		this.channel = "discord:notify";

		this.instance.getServer().getMessenger().registerOutgoingPluginChannel(this.instance, this.channel);
		this.instance.getServer().getMessenger().registerIncomingPluginChannel(this.instance, this.channel, this);
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {

		if (!channel.equalsIgnoreCase(this.channel))
			return;

		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subChannel = in.readUTF();

		if (subChannel.equalsIgnoreCase("GET_GROUPS_REQUEST")) {
			
			UUID uuid = UUID.fromString(in.readUTF());
			
			String[] groups = this.instance.getPermissionsAPI().getGroups(uuid);
			this.sendPlayerGroups(player, groups);
		
		}else if (subChannel.equalsIgnoreCase("GET_PRIMARYGROUP_REQUEST")) {
			
			UUID uuid = UUID.fromString(in.readUTF());

			String[] groups = new String[]{ this.instance.getPermissionsAPI().getPrimaryGroup(uuid) };
			this.sendPlayerGroups(player, groups);
		
		}else if (subChannel.equalsIgnoreCase("INFO_UPDATE")) {

			UUID uuid = UUID.fromString(in.readUTF());
			InformationType type = InformationType.valueOf(in.readUTF());
			
			if(type.equals(InformationType.LastConnection) || type.equals(InformationType.Playtime)) {
				long value = in.readLong();
				this.instance.getOfflineInformationManager().setInformation(uuid, type, value);
			}else {
				String value = in.readUTF();
				this.instance.getOfflineInformationManager().setInformation(uuid, type, value);
			}
		}else if (subChannel.equalsIgnoreCase("GROUP_ACTION")) {
			
			UUID targetUUID = UUID.fromString(in.readUTF());
			GroupAction action = GroupAction.valueOf(in.readUTF().toUpperCase());
			String groupS = in.readUTF();
			
			String[] groups = groupS.split(", ");
			
			for(String group : groups) {
				if(action == GroupAction.ADD)
					this.instance.getPermissionsAPI().addGroup(targetUUID, group);
				else if(action == GroupAction.REMOVE)
					this.instance.getPermissionsAPI().removeGroup(targetUUID, group);
			}
		
		}

	}

	public void sendInformationUpdate(Player p, InformationType type, String value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(type.toString());
		out.writeUTF(value);

		p.sendPluginMessage(this.instance, this.channel, out.toByteArray());
	}
	
	public void sendInformationUpdate(Player p, InformationType type, long value) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("INFO_UPDATE");
		out.writeUTF(type.toString());
		out.writeLong(value);

		p.sendPluginMessage(this.instance, this.channel, out.toByteArray());
	}
	
	public void sendPlayerDeath(Player p, String deathMessage) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("DEATH");
		out.writeUTF(deathMessage);

		p.sendPluginMessage(this.instance, this.channel, out.toByteArray());
	}
	
	public void sendServerStatus(Player p, String server, boolean status) {

		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("SERVER_STATUS");
		out.writeUTF(server);
		out.writeBoolean(status);

		p.sendPluginMessage(this.instance, this.channel, out.toByteArray());
	}
	
	public void sendPlayerGroups(Player p, String[] groups) {

		String groupS = "";
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		
		for(String group : groups) {
			groupS += ", " + group;
		}
		groupS = groupS.substring(2, groupS.length());

		out.writeUTF("GET_GROUPS_ANSWER");
		out.writeUTF(groupS);

		p.sendPluginMessage(this.instance, this.channel, out.toByteArray());
	}
}
