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
import me.truemb.disnotify.manager.OfflineInformationManager;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PlayerManager;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.Member;

public class DC_PlayerInfoCommand extends SimpleAddon {

	private DiscordManager discordManager;
	private ConfigCacheHandler configCache;
	private OfflineInformationManager offlineInfoManager;
	private PluginInformations pluginInfo;
	
    public DC_PlayerInfoCommand(DiscordManager discordManager, ConfigCacheHandler configCache, OfflineInformationManager offlineInfoManager, PluginInformations pluginInfo) {
        super("Disnotify PlayerInfo", "disnotify::playerinfo", "TrueMB", pluginInfo.getPluginVersion(), new String[] { "playerinfo", "pi" });
        this.discordManager = discordManager;
        this.configCache = configCache;
        this.offlineInfoManager = offlineInfoManager;
        this.pluginInfo = pluginInfo;
    }
    
    // /pi <IngameName/uuid>
    
    @Override
    public void onCommand(DiscordBotCommand command, String[] args) {
    	Member member = command.getSender();
    	HashMap<String, String> placeholder = new HashMap<>();
    	placeholder.put("Prefix", command.getPrefix());
    	placeholder.put("Tag", member.getUser().getAsTag());
    	
    	if(args.length == 1) {

    		String ingameNameOrUUID = args[0];
    		
    		new Thread(() -> {
    			UUID uuid = ingameNameOrUUID.length() <= 16 ? PlayerManager.getUUIDOffline(ingameNameOrUUID) : UUID.fromString(ingameNameOrUUID); //NEEDS TIME TO LOAD
				
				if(uuid == null) {
	    			command.getMessage().addReaction("�?�").submit();
					return;
				}

				SimpleDateFormat sdf = new SimpleDateFormat(configCache.getOptionString("DateFormat.Date") + " " + configCache.getOptionString("DateFormat.Time"));

				String username = ingameNameOrUUID.length() <= 16 ? ingameNameOrUUID : null;
				if(username == null)
					username = PlayerManager.getName(uuid.toString());
				
				//MORE SPECIFIC INFORMATIONS
				long lastplayed;
				double playtimeHours;
				double offlinetimeHours;
				
				if(!pluginInfo.isBungeeCordSubServer() && !pluginInfo.isBungeeCord()) {
					//IF NOT BUNGEE SYSTEM
					org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
					lastplayed = player.isOnline() ? System.currentTimeMillis() : player.getLastPlayed();
					playtimeHours = (double) player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60 / 60;
					offlinetimeHours = (double) (System.currentTimeMillis() - player.getLastPlayed()) / 1000 / 60 / 60;
				}else {
					lastplayed = net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid) != null ? System.currentTimeMillis() : this.offlineInfoManager.getInformationLong(uuid, InformationType.LastConnection);
					playtimeHours = (double) this.offlineInfoManager.getInformationLong(uuid, InformationType.Playtime) / 20 / 60 / 60;
					offlinetimeHours = (double) (System.currentTimeMillis() - this.offlineInfoManager.getInformationLong(uuid, InformationType.LastConnection)) / 1000 / 60 / 60;
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
					placeholder.put("Playtime", String.format("%,.2f", playtimeHours) + "h");
					placeholder.put("Offlinetime", String.format("%,.2f", offlinetimeHours) + "h");
					placeholder.put("LastSeen", sdf.format(new Date(lastplayed)) + "h");
				}else {
					placeholder.put("Playtime", "0h");
					placeholder.put("Offlinetime", "0h");
					placeholder.put("LastSeen", "never");
				}

				if(configCache.useEmbedMessage(FeatureType.PlayerInfo)) {
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
