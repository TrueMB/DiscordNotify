package me.truemb.discordnotify.database.connector;

public class DatabaseDriverMariaDB extends DatabaseDriver{

	@Override
	public DatabaseStorage getType() {
		return DatabaseStorage.MARIADB;
	}

	@Override
	public String getDriverClass() {
        return "org.mariadb.jdbc.Driver";
	}

	@Override
	public String getJdbcIdentifier() {
		return "mariadb";
	}

}
