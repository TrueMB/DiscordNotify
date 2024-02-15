package me.truemb.discordnotify.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.spicord.bot.DiscordBot;
import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.discordnotify.enums.DelayType;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.GroupAction;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.utils.DiscordManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class VerifySQL {

	private DiscordNotifyMain instance;
	
	private String table = "disnotify_verify";
	
	public VerifySQL(DiscordNotifyMain plugin){
		this.instance = plugin;
		
		plugin.getAsyncMySql().queryUpdate("CREATE TABLE IF NOT EXISTS " + this.table + " (mcuuid VARCHAR(60) PRIMARY KEY, ingamename VARCHAR(18), disuuid BIGINT UNIQUE KEY, roles TEXT, lastchange TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
	}
	
	public void setVerfied(UUID mcuuid, String ingameName, long disuuid){
		instance.getAsyncMySql().queryUpdate("INSERT INTO " + this.table + " (mcuuid, ingamename, disuuid) VALUES ('" + mcuuid.toString() + "', '" + ingameName + "', '" + String.valueOf(disuuid) + "') "
				+ "ON DUPLICATE KEY UPDATE ingamename=ingamename;");
	}
	
	//code = CONCAT(code, '_standard')
	public void updateRoles(UUID mcuuid, List<String> rolesBackup){
		
		String s = "";
		for(String roles : rolesBackup)
			s += ", " + roles;
		if(s.length() >= 2)
			s = s.substring(2, s.length());
		
		instance.getAsyncMySql().queryUpdate("UPDATE " + this.table + " SET roles='" + s + "'  WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(UUID mcuuid){
		instance.getAsyncMySql().queryUpdate("DELETE FROM " + this.table + " WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(long disUUID){
		instance.getAsyncMySql().queryUpdate("DELETE FROM " + this.table + " WHERE disuuid='" + String.valueOf(disUUID) + "';");
	}
	
	public void checkIfAlreadyVerified(DiscordManager discordManager, DiscordBotCommand cmd, Member member, UUID mcuuid) {
		
		instance.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + mcuuid + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

			    	HashMap<String, String> placeholder = new HashMap<>();
			    	if(discordManager.getDiscordBot() != null)
			    		placeholder.put("Prefix", discordManager.getDiscordBot().getCommandPrefix());
			    	placeholder.put("Tag", member.getUser().getAsTag());
			    	
					while (rs.next()) {
						//ALREADY AUTHENTICATED
				    	cmd.reply(discordManager.getDiscordMessage("verification.minecraftAlreadyAuthenticated", placeholder));
						return;
					}

					//UPDATING CACHE
					instance.getDelayManager().setDelay(member.getIdLong(), DelayType.VERIFY, System.currentTimeMillis() + instance.getConfigManager().getConfig().getInt("Options." + FeatureType.Verification.toString() + ".delayForNewRequest") * 1000);
					
					//REQUESTING
					instance.getVerifyManager().setVerficationProgress(mcuuid, member.getIdLong());
			    	cmd.reply(discordManager.getDiscordMessage("verification.request", placeholder));
			    	
			    	//MINECRAFT CLICK MESSAGE
			    	TextComponent textComponent = Component
			    			.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.message", false).replaceAll("(?i)%user%", member.getUser().getAsTag()) + "\n")
			    				.append(
			    					Component.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.accept", false))
			    					.clickEvent(ClickEvent.runCommand("/verify accept"))
			    					.hoverEvent(HoverEvent.showText(Component.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.acceptHover", false))))
			    				).append(
					    			Component.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.space", false))
					    		).append(
				    				Component.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.deny", false))
				    				.clickEvent(ClickEvent.runCommand("/verify deny"))
				    				.hoverEvent(HoverEvent.showText(Component.text(instance.getConfigManager().getMinecraftMessage("verification.requestClickMessage.denyHover", false))))
				    			);
			    	
			    	instance.getUniversalServer().getPlayer(mcuuid).sendMessage(textComponent);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void acceptVerification(DiscordManager discordManager, UUID uuid, String ingameName) {
		long disuuid = this.instance.getVerifyManager().getVerficationProgress(uuid);
		
		instance.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + uuid + "' OR disuuid ='" + String.valueOf(disuuid) + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					DiscordBot discordBot = discordManager.getDiscordBot();
					if(discordBot == null)
						return;
				
					long discordServerId = instance.getConfigManager().getConfig().getLong("Options.DiscordBot.ServerID");
					Guild guild = discordServerId <= 0 ? discordBot.getJda().getGuilds().get(0) : discordBot.getJda().getGuildById(discordServerId);
					
					while (rs.next()) {
						//SOMETHING GOT AUTHENTICATED
						instance.getUniversalServer().getPlayer(uuid).sendMessage(discordManager.getPlaceholderString(instance.getConfigManager().getMinecraftMessage("verification.otherReasonVerification", true), null));
						return;
					}
					
					//NOT AUTHENTICATED
					setVerfied(uuid, ingameName, disuuid);
					instance.getVerifyManager().setVerified(uuid, disuuid);
					instance.getVerifyManager().clearVerficationProgress(uuid);
					instance.getPluginMessenger().sendPlayerVerified(uuid, disuuid);
					
					Member member = guild.getMemberById(disuuid);
					if(member == null)
						member = guild.retrieveMemberById(disuuid).complete();

					String verfiedGroupS = instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".discordRole");
					Role verifyRole = null;
					
					if (verfiedGroupS.matches("[0-9]+")) {
						Long verifiedGroupId = Long.parseLong(verfiedGroupS);
						verifyRole = discordBot.getJda().getRoleById(verifiedGroupId);
					}else {
						List<Role> verifyRoles = discordBot.getJda().getRolesByName(verfiedGroupS, true);
						if(verifyRoles.size() > 0)
							verifyRole = verifyRoles.get(0);
					}
					
					if(verifyRole == null) {
						instance.getUniversalServer().getLogger().warning("Verify Role couldn't be found. Config Value: '" + verfiedGroupS + "'");
						return;
					}
					
					verifyRole.getGuild().addRoleToMember(member, verifyRole).queue();

					//NICKNAME
					if(instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Verification.toString() +  ".changeNickname")) {
						String nickname = instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".formatNickname").replaceAll("(?i)%" + "user" + "%", ingameName);

						try {
							member.modifyNickname(nickname).queue();
						}catch(HierarchyException ex) {
							instance.getUniversalServer().getLogger().warning("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant change the Nickname.");
						}
					}
					
					//IF A MINECRAFT RANK WAS SET, THEN GIVE THE PLAYER THAT ONE
					String verifyGroupS = instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".minecraftRank");
					if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
						
						String[] array = verifyGroupS.split(":");
					
						if(array.length == 2) {
							String minecraftRank = array[1];
						
							if(instance.getUniversalServer().isProxy() && array[0].equalsIgnoreCase("s") || instance.getPermsAPI().usePluginBridge) {
								String[] groups = { minecraftRank };
								instance.getPluginMessenger().sendGroupAction(uuid, GroupAction.ADD, groups);
							}else {
								instance.getPermsAPI().addGroup(uuid, minecraftRank);
							}
							
						}else {
							instance.getUniversalServer().getLogger().warning("Something went wrong with adding the Verificationsgroup on Minecraft!");
						}
					}
					
					//RUN COMMANDS ON THE LIST ON THE MAIN SERVER
					List<String> commands = instance.getConfigManager().getConfig().getStringList("Options." + FeatureType.Verification.toString() +  ".commands");
					if(commands != null) {
						commands.forEach(command -> instance.getUniversalServer().sendCommandToConsole(command
								.replaceAll("(?i)%" + "player" + "%", ingameName)
								.replaceAll("(?i)%" + "uuid" + "%", uuid.toString())
						));
					}
					
					instance.getUniversalServer().getPlayer(uuid).sendMessage(discordManager.getPlaceholderString(instance.getConfigManager().getMinecraftMessage("verification.accept", true), null));
					
					//ROLE SYNC
					if(instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync)) {
						
						boolean usePrimaryGroup = instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useOnlyPrimaryGroup");
						
						//ASK FOR GROUPS, IF NO PERMISSION SYSTEM FOUND ON BUNGEE (Maybe using Vault)
						if(instance.getPermsAPI().usePluginBridge) {
							if(instance.getPluginMessenger() != null) {
								if(usePrimaryGroup)
									instance.getPluginMessenger().askForPrimaryGroup(uuid);
								else
									instance.getPluginMessenger().askForGroups(uuid);
							}
						}else {
							
							String[] currentGroupList;
	
							if(usePrimaryGroup)
								currentGroupList = new String[]{ instance.getPermsAPI().getPrimaryGroup(uuid) };
							else
								currentGroupList = instance.getPermsAPI().getGroups(uuid);
							
							if(instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft")) {
								instance.getDiscordManager().syncRolesFromDiscord(uuid, member);
							}else {
								instance.getDiscordManager().syncRolesFromMinecraft(uuid, member, currentGroupList);
							}
						}
						
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupVerifications() {
		
		instance.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						long disuuid = rs.getLong("disuuid");
						UUID mcuuid = UUID.fromString(rs.getString("mcuuid"));
						
						List<String> roleList = new ArrayList<>();
						String rolesS = rs.getString("roles");
						
						if(rolesS != null && !rolesS.equals("")) {
							String[] roles = rolesS.split(", ");
							for(String role : roles)
								roleList.add(role);
	
							instance.getVerifyManager().setBackupRoles(mcuuid, roleList);
						}
						
						instance.getVerifyManager().setVerified(mcuuid, disuuid);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
