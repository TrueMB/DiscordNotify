package me.truemb.discordnotify.main;

public class PluginDescription {
	
	private String name;
	private String author;
	private String version;
	
	public PluginDescription(String name, String author, String version) {
		this.name = name;
		this.author = author;
		this.version = version;
	}

	public String getName() {
		return this.name;
	}

	public String getAuthor() {
		return this.author;
	}

	public String getVersion() {
		return this.version;
	}

}
