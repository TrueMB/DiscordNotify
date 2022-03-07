package me.truemb.discordnotify.messaging;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import me.truemb.discordnotify.enums.GroupAction;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.messenger.IPipeline;
import me.truemb.universal.messenger.IPipelineRegistry;
import me.truemb.universal.messenger.MessageChannelAPI;
import me.truemb.universal.messenger.PipelineMessage;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.entities.Member;

public class PluginMessenger {
	
	public static final String channelName = "discord:notify";
	
	private DiscordNotifyMain instance;
	private IPipeline pipeline;
	
	public PluginMessenger(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		IPipelineRegistry registry = MessageChannelAPI.getPipelineRegistry();
		this.pipeline = registry.registerAsync(plugin, PluginMessenger.channelName);
		this.pipeline.onReceive(this.onReceive());
	}
	
	public Consumer<PipelineMessage> onReceive() {
		return new Consumer<PipelineMessage>() {

			@Override
			public void accept(PipelineMessage message) {

				List<Object> rows = message.getContents();

				if(rows.size() <= 0)
					return;
				
				String subChannel = (String) rows.get(0);
				
				if (subChannel.equalsIgnoreCase("DEATH")) {
					
					UUID uuid = message.getTarget();
					UniversalPlayer up = instance.getUniversalServer().getPlayer(uuid);
					String deathMessage = (String) rows.get(1);
					
					instance.getListener().onPlayerDeath(up, deathMessage);
					
				}else if (subChannel.equalsIgnoreCase("GET_GROUPS_ANSWER")) {
					
					UUID uuid = message.getTarget();
					String groupS = (String) rows.get(1);
					String[] currentGroupList = groupS.split(", ");
					
					long disuuid = instance.getVerifyManager().getVerfiedWith(uuid);
					
					//ACCEPTING REQUEST
					Member member = instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
					if(member == null)
						instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).queue(mem ->  
							instance.getVerifyManager().checkForRolesUpdate(uuid, mem, currentGroupList));
					else
						instance.getVerifyManager().checkForRolesUpdate(uuid, member, currentGroupList);
				
				}else if (subChannel.equalsIgnoreCase("INFO_UPDATE")) {

					UUID uuid = message.getTarget();
					InformationType type = InformationType.valueOf((String) rows.get(1));
					
					if(type.equals(InformationType.LastConnection) || type.equals(InformationType.Playtime)) {
						long value = (long) rows.get(2);
						instance.getOfflineInformationManager().setInformation(uuid, type, value);
					}else {
						String value = (String) rows.get(2);
						instance.getOfflineInformationManager().setInformation(uuid, type, value);
					}
				}else if (subChannel.equalsIgnoreCase("SERVER_STATUS")) {
					//ONLY SENDS MESSAGE, IF PLAYER ON SERVER. MAKES NO SENSE FOR SERVER START AND STOP
					
					//String server = in.readUTF();
					//boolean status = in.readBoolean();
					
				}else if (subChannel.equalsIgnoreCase("GET_GROUPS_REQUEST")) {
						
					UUID uuid = message.getTarget();
						
					String[] groups = instance.getPermsAPI().getGroups(uuid);
					sendPlayerGroups(uuid, groups);
				
				}else if (subChannel.equalsIgnoreCase("GET_PRIMARYGROUP_REQUEST")) {

					UUID uuid = message.getTarget();

					String[] groups = new String[]{ instance.getPermsAPI().getPrimaryGroup(uuid) };
					sendPlayerGroups(uuid, groups);
				
				}else if (subChannel.equalsIgnoreCase("INFO_UPDATE")) {

					UUID uuid = message.getTarget();
					InformationType type = InformationType.valueOf((String) rows.get(1));
					
					if(type.equals(InformationType.LastConnection) || type.equals(InformationType.Playtime)) {
						long value = (long) rows.get(2);
						instance.getOfflineInformationManager().setInformation(uuid, type, value);
					}else {
						String value = (String) rows.get(2);
						instance.getOfflineInformationManager().setInformation(uuid, type, value);
					}
				}else if (subChannel.equalsIgnoreCase("GROUP_ACTION")) {
						
					UUID targetUUID = message.getTarget();
					GroupAction action = GroupAction.valueOf((String) rows.get(1));
					String groupS = (String) rows.get(2);
					
					String[] groups = groupS.split(", ");
					
					for(String group : groups) {
						if(action == GroupAction.ADD)
							instance.getPermsAPI().addGroup(targetUUID, group);
						else if(action == GroupAction.REMOVE)
							instance.getPermsAPI().removeGroup(targetUUID, group);
					}
				
				}
			}
			
		};
		
	}

	//ASKS WHAT GROUPS THE UUID GOT
	public void askForGroups(UUID uuid) {
		PipelineMessage message = new PipelineMessage(uuid);

		message.write("GET_GROUPS_REQUEST");
		
		this.pipeline.send(message);
	}
	
	//ASKS WHAT PRIMARY GROUP THE UUID GOT
	public void askForPrimaryGroup(UUID uuid) {
		PipelineMessage message = new PipelineMessage(uuid);

		message.write("GET_PRIMARYGROUP_REQUEST");
		
		this.pipeline.send(message);
	}
	
	public void sendGroupAction(UUID uuid, GroupAction action, String[] groups) {
	    
	    String groupS = "";
		for(String group : groups)
			groupS += ", " + group;
		groupS = groupS.substring(2, groupS.length());

		PipelineMessage message = new PipelineMessage(uuid);

		message.write("INFO_UPDATE");
		message.write(action.toString()); 
		message.write(groupS);
		
		this.pipeline.send(message);
	}
	
	public void sendInformationUpdate(UUID uuid, InformationType type, Object value) {
		 this.sendInformationUpdate(uuid, null, type, value);
	}
	
	public void sendInformationUpdate(UUID uuid, String server, InformationType type, Object value) {
		PipelineMessage message = new PipelineMessage(uuid, server);

		message.write("INFO_UPDATE");
		message.write(type.toString());
		message.write(value);
		
		this.pipeline.send(message);
	}

	public void sendPlayerDeath(UUID uuid, String deathMessage) {

		PipelineMessage message = new PipelineMessage(uuid);
		
		message.write("DEATH");
		message.write(deathMessage);
		
		this.pipeline.send(message);
	}
	
	public void sendServerStatus( String server, boolean status) {

		PipelineMessage message = new PipelineMessage();
		
		message.write("SERVER_STATUS");
		message.write(server);
		message.write(status);
		
		this.pipeline.send(message);
	}
	
	public void sendPlayerGroups(UUID uuid, String[] groups) {

		String groupS = "";
		for(String group : groups)
			groupS += ", " + group;
		groupS = groupS.substring(2, groupS.length());
		
		PipelineMessage message = new PipelineMessage(uuid);
		
		message.write("GET_GROUPS_ANSWER");
		message.write(groupS);
		
		this.pipeline.send(message);
	}

}
