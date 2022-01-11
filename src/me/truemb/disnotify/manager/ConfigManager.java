package me.truemb.disnotify.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleyaml.configuration.file.YamlConfiguration;

import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.utils.ConfigUpdater;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class ConfigManager {

	private static final int configVersion = 8;
	
	private File configFile;
	private YamlConfiguration config;
	
	public ConfigManager(Logger logger, InputStream pluginConfig, File dir) {
		
		String filename = "config.yml";
		this.configFile = new File(dir, filename);
		
		//CREATES CONFIG, IF IT DOESNT EXISTS
		if (!this.configFile.exists()){
			
			if(!dir.exists())
				dir.mkdir();
			
			try (InputStream in = pluginConfig) {
		        Files.copy(in, this.configFile.toPath());
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
			
		//LOAD ONCE TO GET THE CONFIG VERSION
		this.config = YamlConfiguration.loadConfiguration(this.configFile);
			
		//UPDATE IF THERE IS A NEWER VERSION
		if(!this.config.isSet("ConfigVersion") || this.config.getInt("ConfigVersion") < configVersion) {
			logger.info("Updating Config!");
			try {
				ConfigUpdater.update(pluginConfig, this.configFile, new ArrayList<>());
				this.config = YamlConfiguration.loadConfiguration(this.configFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public YamlConfiguration getConfig() {
		return this.config;
	}
	
	public boolean isFeatureEnabled(FeatureType type) {
		return this.getConfig().getBoolean("FeaturesEnabled." + type);
	}
	
	public boolean useEmbedMessage(FeatureType type) {
		return this.getConfig().getBoolean("Options." + type + ".useEmbedMessage");
	}
	
	
	public long getChannelID(FeatureType type) {
		return this.getConfig().getLong("Channel." + type);
	}

	public TextComponent getMessageAsTextComponent(String path, boolean prefix) {
		return new TextComponent(this.getMinecraftMessage(path, prefix));
	}
	
	public String getMinecraftMessage(String path, boolean prefix) {
		return this.translateHexColorCodes(this.getConfig().getString("Messages." + path));
	}
		
	private String translateHexColorCodes(String message){
		
        final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color) + "");
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
