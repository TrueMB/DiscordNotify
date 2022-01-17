package me.truemb.disnotify.main;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.disnotify.bungeecord.commands.BC_DChatCommand;
import me.truemb.disnotify.bungeecord.commands.BC_StaffCommand;
import me.truemb.disnotify.bungeecord.commands.BC_VerifyCommand;
import me.truemb.disnotify.commands.minecraft.DChat_Command;
import me.truemb.disnotify.commands.minecraft.Staff_Command;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;
import net.md_5.bungee.api.ProxyServer;

public class DiscordNotifyPlugin {
	
	public DiscordNotifyPlugin() {
		// TODO Auto-generated constructor stub
	}
	
	//DISCORD BOT
	private DiscordManager discordMGR;

    //CACHE
    public HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
    public HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
    
    //HANDLER
	private ConfigManager configManager;
	
	
	
	
	private void setupCommands() {

		//COMMANDS
		if(this.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat"))
			new DChat_Command(this).register(this);
		
		if(this.getConfigManager().isFeatureEnabled(FeatureType.Staff))
			new Staff_Command(this).register(this);
		
		BC_VerifyCommand verifyCmd = new BC_VerifyCommand(this.getDiscordManager(), this.getConfigManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getMessagingManager(), this.getPermissionsAPI());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, verifyCmd);
				
	}
	

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public DiscordManager getDiscordManager() {
		return discordMGR;
	}
	

}
