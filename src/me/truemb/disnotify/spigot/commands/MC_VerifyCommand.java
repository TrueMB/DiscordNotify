package me.truemb.disnotify.spigot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class MC_VerifyCommand extends BukkitCommand implements TabCompleter{

	private ConfigManager configManager;
	private PluginInformations pluginInfo;
	private DiscordManager discordManager;
	private VerifyManager verifyManager;
	private VerifySQL verifySQL;
	private PermissionsAPI permsAPI;
	
	private List<String> arguments = new ArrayList<>();
	
	public MC_VerifyCommand(DiscordManager discordManager, ConfigManager configManager, PluginInformations pluginInfo, VerifyManager verifyManager, VerifySQL verifySQL, PermissionsAPI permsAPI) {
		super("verify");
		this.configManager = configManager;
		this.pluginInfo = pluginInfo;
		this.discordManager = discordManager;
		this.verifyManager = verifyManager;
		this.verifySQL = verifySQL;
		this.permsAPI = permsAPI;
		
		this.arguments.add("unlink");
		this.arguments.add("accept");
		this.arguments.add("deny");
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.configManager.getMinecraftMessage("console", false));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();

		if(!this.discordManager.isAddonEnabled("disnotify::verify")) {
			p.sendMessage(this.configManager.getMinecraftMessage("disabledFeature", true));
			return true;
		}
		
		if(args.length == 1){
					
			if(args[0].equalsIgnoreCase("unlink")) {
				
				if(!this.verifyManager.isVerified(uuid)) {
					p.sendMessage(this.configManager.getMinecraftMessage("verification.notVerified", true));
					return true;
				}
				
				if(this.discordManager.getDiscordBot() == null || this.discordManager.getDiscordBot().getJda() == null || this.discordManager.getDiscordBot().getJda().getGuilds().size() <= 0) {
					p.sendMessage(this.configManager.getMinecraftMessage("verification.botNotReady", true));
					return true;
				}
				
				//UNLINK
				long disuuid = this.verifyManager.getVerfiedWith(uuid);
				Member member = this.discordManager.getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
				if(member == null)
					member = this.discordManager.getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).complete();
				
				//REMOVE VERIFY ROLE
				List<Role> verifyRoles = this.discordManager.getDiscordBot().getJda().getRolesByName(this.configManager.getConfig().getString("Options." + FeatureType.Verification.toString() + ".discordRole"), true);
				if(verifyRoles.size() > 0) {
    				Role verifyRole = verifyRoles.get(0);
    				verifyRole.getGuild().removeRoleFromMember(member, verifyRole).complete();
				}
				
				//NICKNAME
				if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Verification.toString() + ".changeNickname")) {
					try {
						member.modifyNickname(null).complete();
					}catch(HierarchyException ex) {
						this.pluginInfo.getLogger().info("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant change the Nickname.");
					}
				}
				
				//RESET ROLES
				DisnotifyTools.resetRoles(uuid, member, this.configManager, this.verifyManager, this.discordManager);
				
				String verifyGroupS = this.configManager.getConfig().getString("Options." + FeatureType.Verification.toString() +  ".minecraftRank");
				
				if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
					
					String[] array = verifyGroupS.split(":");
				
					if(array.length == 2) {
						String minecraftRank = array[1];

						if(this.pluginInfo.isBungeeCord() && array[0].equalsIgnoreCase("s") || this.permsAPI.usePluginBridge) {
							String[] groups = { minecraftRank };
							PluginMessagingBungeecordManager.sendGroupAction(net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid), GroupAction.REMOVE, groups);
						}else {
							this.permsAPI.removeGroup(uuid, minecraftRank);
						}
						
					}else {
						this.pluginInfo.getLogger().warning("Something went wrong with removing the Verificationsgroup on Minecraft!");
					}
				}
				
				this.verifyManager.removeVerified(uuid);
				this.verifySQL.deleteVerification(uuid);
				p.sendMessage(this.configManager.getMinecraftMessage("verification.unlinked", true));
				return true;
				
			}else if(args[0].equalsIgnoreCase("accept")) {

				if(!this.verifyManager.isVerficationInProgress(uuid)) {
					p.sendMessage(this.configManager.getMinecraftMessage("verification.sessionTimeOut", true));
					return true;
				}
				
				//ACCEPTING REQUEST
				this.verifySQL.acceptVerification(this.discordManager, uuid, p.getName(), this.permsAPI.getGroups(uuid));
				return true;
				
			}else if(args[0].equalsIgnoreCase("deny")) {

				if(!this.verifyManager.isVerficationInProgress(uuid)) {
					p.sendMessage(this.configManager.getMinecraftMessage("verification.nothingToDeny", true));
					return true;
				}
				
				//DENING REQUEST
				this.verifyManager.clearVerficationProgress(uuid);
				
				p.sendMessage(this.configManager.getMinecraftMessage("verification.denied", true));
				return true;
				
			}
		}

		p.sendMessage(this.configManager.getMinecraftMessage("verification.help", true));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> result = new ArrayList<>();
		

		if(args.length == 1) {
			for(String subCMD : this.arguments)
				if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
					result.add(subCMD);
		}
		
		return result;
	}

}