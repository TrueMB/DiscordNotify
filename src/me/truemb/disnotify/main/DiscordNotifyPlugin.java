package me.truemb.disnotify.main;

import java.util.HashMap;
import java.util.UUID;

import eu.mcdb.shaded.universal.command.api.CommandParameter;
import me.truemb.disnotify.commands.minecraft.DChat_Command;
import me.truemb.disnotify.commands.minecraft.Staff_Command;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;

public class DiscordNotifyPlugin {
	
	//MAIN CLASS
	private Object pluginInstance;
	
	//DISCORD BOT
	private DiscordManager discordManager;

    //HANDLER
	private ConfigManager configManager;
	
    //CACHE
    public HashMap<UUID, Boolean> staffChatDisabled = new HashMap<>();
    public HashMap<UUID, Boolean> discordChatEnabled = new HashMap<>();
    
	
	public DiscordNotifyPlugin(Object pluginInstance, ConfigManager configManager, DiscordManager discordManager) {
		this.pluginInstance = pluginInstance;
		this.configManager = configManager;
		this.discordManager = discordManager;
		this.setupCommands();
	}
	
	
	private void setupCommands() {

		//COMMANDS
		if(this.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat"))
			new DChat_Command(this).register(pluginInstance);
		
		if(this.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			Staff_Command staffCommand = new Staff_Command(this);
			//for(int i = 0; i < 100; i++)
			//	staffCommand.setParameter(i, new CommandParameter("Message"));
			staffCommand.setParameter(1, new CommandParameter("Message"));
			staffCommand.register(pluginInstance);
		}
		
		//BC_VerifyCommand verifyCmd = new BC_VerifyCommand(this.getDiscordManager(), this.getConfigManager(), this.getPluginInformations(), this.getVerifyManager(), this.getVerifySQL(), this.getMessagingManager(), this.getPermissionsAPI());
		//ProxyServer.getInstance().getPluginManager().registerCommand(this, verifyCmd);
				
	}
	

	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	public DiscordManager getDiscordManager() {
		return this.discordManager;
	}
	

}
