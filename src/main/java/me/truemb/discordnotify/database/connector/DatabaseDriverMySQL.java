package me.truemb.discordnotify.database.connector;

public class DatabaseDriverMySQL extends DatabaseDriver{

	@Override
	public DatabaseStorage getType() {
		return DatabaseStorage.MYSQL;
	}

	@Override
	public String getDriverClass() {
		return "com.mysql.cj.jdbc.Driver";
	}

	@Override
	public String getJdbcIdentifier() {
		return "mysql";
	}

}
