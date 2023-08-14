package me.truemb.discordnotify.database.connector;

public abstract class DatabaseDriver {
	
	public abstract DatabaseStorage getType();
	
	public abstract String getDriverClass();
	
	public abstract String getJdbcIdentifier();

}
