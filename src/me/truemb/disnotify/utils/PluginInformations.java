package me.truemb.disnotify.utils;

import java.util.logging.Logger;

public class PluginInformations {
	
	private String pluginName;
	private String pluginVersion;
	private String serverVersion;
	
	private boolean isBungeeCord;
	private boolean isBungeeCordSubServer;
	
	private Logger logger;
	
	public PluginInformations(String pluginName, String pluginVersion, String serverVersion, Logger logger, boolean isBungeeCord, boolean isBungeeCordSubServer) {
		this.pluginName = pluginName;
		this.pluginVersion = pluginVersion;
		this.serverVersion = serverVersion;
		
		this.logger = logger;
		
		this.isBungeeCord = isBungeeCord;
		this.isBungeeCordSubServer = isBungeeCordSubServer;
		
	}

	public String getPluginName() {
		return this.pluginName;
	}
	
	public String getPluginVersion() {
		return this.pluginVersion;
	}

	public String getServerVersion() {
		return this.serverVersion;
	}

	public boolean isBungeeCord() {
		return this.isBungeeCord;
	}

	public boolean isBungeeCordSubServer() {
		return this.isBungeeCordSubServer;
	}

	public Logger getLogger() {
		return this.logger;
	}

}
