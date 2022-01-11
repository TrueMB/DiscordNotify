package me.truemb.disnotify.discord.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PlayerManager;
import me.truemb.disnotify.utils.PluginInformations;
import me.truemb.disnotify.utils.TimeFormatter;
import net.dv8tion.jda.api.entities.Member;

public class DC_PlayerInfoCommand extends SimpleAddon {

	private DiscordManager discordManager;
	private ConfigManager configManager;
	private OfflineInformationManager offlineInfoManager;
	private PluginInformations pluginInfo;
	
    public DC_PlayerInfoCommand(DiscordManager discordManager, ConfigManager configManager, OfflineInformationManager offlineInfoManager, PluginInformations pluginInfo) {
        super("Disnotify PlayerInfo", "disnotify::playerinfo", "TrueMB", pluginInfo.getPluginVersion(), new String[] { "playerinfo", "pi" });
        this.discordManager = discordManager;
        this.configManager = configManager;
        this.offlineInfoManager = offlineInfoManager;
        this.pluginInfo = pluginInfo;
    }
    
    // /pi <IngameName/uuid>
    
    @Override
    public void onCommand(DiscordBotCommand command, String[] args) {
    	
    	Member member = command.getSender();
    	long channelID = command.getChannel().getIdLong();
    	
    	HashMap<String, String> placeholder = new HashMap<>();
    	placeholder.put("Prefix", command.getPrefix());
    	placeholder.put("Tag", member.getUser().getAsTag());

    	
    	long commandAllowedChannelID = this.configManager.getConfig().getLong("Options." + FeatureType.PlayerInfo.toString() + ".discordCommandOnlyInChannel");
    	
    	if(commandAllowedChannelID != -1 && commandAllowedChannelID != channelID)
    		return;
    	
    	if(args.length == 1) {

    		String ingameNameOrUUID = args[0];
    		
    		new Thread(() -> {
    			UUID uuid = ingameNameOrUUID.length() <= 16 ? PlayerManager.getUUIDOffline(ingameNameOrUUID) : UUID.fromString(ingameNameOrUUID); //NEEDS TIME TO LOAD
				
				if(uuid == null) {
	    			command.getMessage().addReaction("�?�").submit();
					return;
				}

				SimpleDateFormat sdf = new SimpleDateFormat(this.configManager.getConfig().getString("Options.DateFormat.Date") + " " + this.configManager.getConfig().getString("Options.DateFormat.Time"));

				String username = ingameNameOrUUID.length() <= 16 ? ingameNameOrUUID : null;
				if(username == null)
					username = PlayerManager.getName(uuid.toString());
				
				//MORE SPECIFIC INFORMATIONS
				long lastplayed;
				long playtimeSec;
				long offlinetimeSec;
				
				if(!pluginInfo.isBungeeCordSubServer() && !pluginInfo.isBungeeCord()) {
					//IF NOT BUNGEE SYSTEM
					org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
					lastplayed = player.isOnline() ? System.currentTimeMillis() : player.getLastPlayed();
					playtimeSec = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20;
					offlinetimeSec = (System.currentTimeMillis() - player.getLastPlayed()) / 1000;
				}else {
					lastplayed = net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid) != null ? System.currentTimeMillis() : this.offlineInfoManager.getInformationLong(uuid, InformationType.LastConnection);
					playtimeSec = this.offlineInfoManager.getInformationLong(uuid, InformationType.Playtime) / 20;
					offlinetimeSec = (System.currentTimeMillis() - this.offlineInfoManager.getInformationLong(uuid, InformationType.LastConnection)) / 1000;
				}
				
				String location = offlineInfoManager.getInformationString(uuid, InformationType.Location);
				if(location == null) location = "unknown";
				String ip = offlineInfoManager.getInformationString(uuid, InformationType.IP);
				if(ip == null) ip = "unknown";
				String server = offlineInfoManager.getInformationString(uuid, InformationType.Bungee_Server);
				if(server == null) server = "";
									
				placeholder.put("Player", username);
				placeholder.put("UUID", uuid.toString());
				placeholder.put("Server", server);
				placeholder.put("Location", location);
				placeholder.put("IP", ip);
				
				if(lastplayed > 0) {
					placeholder.put("Playtime", TimeFormatter.formatDate(playtimeSec, this.configManager));
					placeholder.put("Offlinetime", TimeFormatter.formatDate(offlinetimeSec, this.configManager));
					placeholder.put("LastSeen", sdf.format(new Date(lastplayed)) + "h");
				}else {
					placeholder.put("Playtime", "unknown");
					placeholder.put("Offlinetime", "unknown");
					placeholder.put("LastSeen", "never");
				}

				if(configManager.useEmbedMessage(FeatureType.PlayerInfo)) {
					this.discordManager.sendEmbedMessage(command.getChannel().getIdLong(), uuid, "PlayerInfoEmbed", placeholder);
		    		//command.reply(discordManager.getEmbedMessage(uuid, "PlayerInfoEmbed", placeholder).build());
				}else {
		    		command.reply(this.discordManager.getDiscordMessage("PlayerInfo", placeholder));
				}
		        command.getMessage().addReaction("✔").submit();
    		}).start();
    		
    	}else {
    		command.reply(this.discordManager.getDiscordMessage("PlayerInfoHelp", placeholder));
    	}
    	
    }

	@Override
	public void onShutdown(DiscordBot bot) {
		this.pluginInfo.getLogger().info("Disabling the PlayerInfo Command.");
	}
}
