package me.truemb.discordnotify.api;

import java.util.UUID;

import org.bukkit.entity.Player;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class PlaceholderAPI extends PlaceholderExpansion{
	
	private DiscordNotifyMain instance;
	
	public PlaceholderAPI(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }
    
	@Override
	public String getAuthor() {
		return this.instance.getPluginDescription().getAuthor();
	}

	@Override
	public String getIdentifier() {
		return this.instance.getPluginDescription().getName();
	}

	@Override
	public String getVersion() {
		return this.instance.getPluginDescription().getVersion();
	}
	
    @Override
    public String onPlaceholderRequest(Player p, String identifier){

        if(p == null || identifier == null)
            return "";
        
        UUID uuid = p.getUniqueId();
        
        if(identifier.equalsIgnoreCase("isVerfied")){
        	
        	boolean verified = this.instance.getVerifyManager().isVerified(uuid);
        	
            return this.instance.getConfigManager().getPlaceholder("isVerified." + (verified ? "true" : "false"));
            
        }
        return null;
    }

}
