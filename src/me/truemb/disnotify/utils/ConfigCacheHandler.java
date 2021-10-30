package me.truemb.disnotify.utils;

import java.util.HashMap;
import java.util.List;

import me.truemb.disnotify.enums.FeatureType;

public class ConfigCacheHandler {

	//This Class Caches some Config values, that needs to be avaible for Bungeecord and Spigot
	//Can only read config values
	
	private HashMap<FeatureType, Boolean> featureEnabled = new HashMap<>(); //feature - enabled
	private HashMap<FeatureType, Long> featureChannelId = new HashMap<>(); //feature - channelID
	
	private HashMap<String, String> options = new HashMap<>(); //path - option
	
	private HashMap<String, String> permissions = new HashMap<>(); //path - permission
	
	private HashMap<String, String> embedHash = new HashMap<>(); //path - Embed Stuff

	private HashMap<String, List<String>> lists = new HashMap<>(); //path - Listen
	
	private HashMap<String, String> minecraftMessagesHash = new HashMap<>(); //path - message
	private HashMap<String, String> discordMessagesHash = new HashMap<>(); //path - message
	
	public ConfigCacheHandler() {
		
	}

	//OPTIONS
	public void addOption(String optionPath, String optionValue) {
		this.options.put(optionPath, optionValue);
	}
	
	//DOESNT NEED OPTIONS. in the path
	public String getOptionString(String path) {
		return this.options.get("Options." + path);
	}

	public int getOptionInt(String path) {
		return Integer.parseInt(this.getOptionString(path));
	}

	public double getOptionDouble(String path) {
		return Double.parseDouble(this.getOptionString(path));
	}
	
	public boolean getOptionBoolean(String path) {
		return this.getOptionString(path) != null ? this.getOptionString(path).equalsIgnoreCase("true") : false;
	}
	
	public boolean useEmbedMessage(FeatureType feature) {
		String useEmbed = this.getOptionString(feature.toString() + ".useEmbedMessage");
		if(useEmbed != null && useEmbed.equalsIgnoreCase("true"))
			return true;
		return false;
	}
	
	//MINECRAFT MESSAGES
	public void addMinecraftMessage(String path, String message) {
		this.minecraftMessagesHash.put(path, message);
	}

	//Messages NEED OPTIONS. in the path
	public String getMinecraftMessage(String path, boolean withPrefix) {
		String message = "";
		if(withPrefix)
			message = this.minecraftMessagesHash.get("Messages.prefix") + " ";
		message += this.minecraftMessagesHash.get("Messages." + path);
		return message;
	}
	
	//Listen
	public void addList(String path, List<String> list) {
		this.lists.put(path, list);
	}

	public List<String> getList(String path) {
		return this.lists.get(path);
	}
	
	//Permissions
	public void addPermission(String path, String permission) {
		this.permissions.put(path, permission);
	}
	
	public String getPermission(String path) {
		return this.permissions.get("Permissions." + path);
	}
	
	//DISCORD MESSAGES
	public void addDiscordMessage(String path, String message) {
		this.discordMessagesHash.put(path, message);
	}
	
	public String getDiscordMessage(String path) {
		return this.discordMessagesHash.get("DiscordMessages." + path);
	}
	
	//EmbedStuff
	public void addEmbedString(String path, String value) {
		this.embedHash.put(path, value);
	}
	
	public String getEmbedString(String path) {
		return this.embedHash.get("DiscordEmbedMessages." + path);
	}
	
	public boolean getEmbedBoolean(String path) {
		return this.getEmbedString(path).equalsIgnoreCase("true");
	}
	
	//FEATURES ENABLED?
	public void setFeatureEnabled(FeatureType type, boolean enabled) {
		this.featureEnabled.put(type, enabled);
	}
	
	public boolean isFeatureEnabled(FeatureType type) {
		return this.featureEnabled.get(type);
	}
	
	//FEATURES ChannelId
	public void setChannelId(FeatureType type, long channelId) {
		this.featureChannelId.put(type, channelId);
	}
	
	public Long getChannelId(FeatureType type) {
		return this.featureChannelId.get(type);
	}
}
