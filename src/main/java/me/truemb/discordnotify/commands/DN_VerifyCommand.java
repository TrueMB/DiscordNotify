package me.truemb.discordnotify.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.GroupAction;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class DN_VerifyCommand {
	
	private DiscordNotifyMain instance;

	@Getter private List<String> arguments = new ArrayList<>();
	
	public DN_VerifyCommand(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		this.arguments.add("unlink");
		this.arguments.add("accept");
		this.arguments.add("deny");
	}
	
	public void onCommand(UniversalPlayer up, String[] args) {

		UUID uuid = up.getUUID();
		
		if(args.length == 1){
					
			if(args[0].equalsIgnoreCase("unlink")) {
				
				if(!this.instance.getVerifyManager().isVerified(uuid)) {
					up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.notVerified", true));
					return;
				}
				
				if(this.instance.getDiscordManager().getDiscordBot() == null || this.instance.getDiscordManager().getDiscordBot().getJda() == null || this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().size() <= 0) {
					up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.botNotReady", true));
					return;
				}
				
				//UNLINK
				long disuuid = this.instance.getVerifyManager().getVerfiedWith(uuid);
				Member member = this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
				if(member == null)
					member = this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).complete();
				
				//REMOVE VERIFY ROLE
				String verfiedGroupS = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() + ".discordRole");
				Role verifyRole = null;
				
				if (verfiedGroupS.matches("[0-9]+")) {
					Long verifiedGroupId = Long.parseLong(verfiedGroupS);
					verifyRole = this.instance.getDiscordManager().getDiscordBot().getJda().getRoleById(verifiedGroupId);
				}else {
					List<Role> verifyRoles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(verfiedGroupS, true);
					if(verifyRoles.size() > 0)
						verifyRole = verifyRoles.get(0);
				}
				
				if(verifyRole == null) {
					this.instance.getUniversalServer().getLogger().warning("Verify Role couldn't be found. Config Value: '" + verfiedGroupS + "'");
					return;
				}
				
    			verifyRole.getGuild().removeRoleFromMember(member, verifyRole).queue();
				
				//NICKNAME
				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Verification.toString() + ".changeNickname")) {
					try {
						member.modifyNickname(null).queue();
					}catch(HierarchyException ex) {
						this.instance.getUniversalServer().getLogger().info("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant change the Nickname.");
					}
				}
				
				//RESET ROLES
				this.instance.getDiscordManager().resetRoles(uuid, member);
				
				String verifyGroupS = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".minecraftRank");
				
				if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
					
					String[] array = verifyGroupS.split(":");
				
					if(array.length == 2) {
						String minecraftRank = array[1];

						if(this.instance.getUniversalServer().isProxy() && array[0].equalsIgnoreCase("s") || this.instance.getPermsAPI().usePluginBridge) {
							String[] groups = { minecraftRank };
							this.instance.getPluginMessenger().sendGroupAction(uuid, GroupAction.REMOVE, groups);
						}else {
							this.instance.getPermsAPI().removeGroup(uuid, minecraftRank);
						}
						
					}else {
						this.instance.getUniversalServer().getLogger().warning("Something went wrong with removing the Verificationsgroup on Minecraft!");
					}
				}
				
				this.instance.getVerifyManager().removeVerified(uuid);
				this.instance.getVerifySQL().deleteVerification(uuid);
				this.instance.getPluginMessenger().sendPlayerUnverified(uuid);
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.unlinked", true));
				return;
				
			}else if(args[0].equalsIgnoreCase("accept")) {

				if(!this.instance.getVerifyManager().isVerficationInProgress(uuid)) {
					up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.sessionTimeOut", true));
					return;
				}
				
				//ACCEPTING REQUEST
				this.instance.getVerifySQL().acceptVerification(this.instance.getDiscordManager(), uuid, up.getIngameName());
				
				return;
				
			}else if(args[0].equalsIgnoreCase("deny")) {

				if(!this.instance.getVerifyManager().isVerficationInProgress(uuid)) {
					up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.nothingToDeny", true));
					return;
				}
				
				//DENING REQUEST
				this.instance.getVerifyManager().clearVerficationProgress(uuid);
				
				up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.denied", true));
				return;
				
			}
		}

		up.sendMessage(this.instance.getConfigManager().getMinecraftMessage("verification.help", true));
		return;
	}
	
}
