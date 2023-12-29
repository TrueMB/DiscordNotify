package me.truemb.discordnotify.database.connector;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import me.truemb.discordnotify.manager.ConfigManager;

public class AsyncMySQL {
	
	private ExecutorService executor;
	private DatabaseConnector sql;
	
	private String databaseName;

	public AsyncMySQL(Logger logger, ConfigManager configManager) throws Exception {

		String type = configManager.getConfig().getString("Database.type");
		DatabaseStorage storage = DatabaseStorage.getStorageFromString(type);
		
		if(storage == null)
			throw new Exception("The Database Storage Type is invalid! '" + type + "'");
			
		
		String host = configManager.getConfig().getString("Database.host");
		int port = configManager.getConfig().getInt("Database.port");
		boolean useSSL = configManager.getConfig().getBoolean("Database.useSSL");
		String user = configManager.getConfig().getString("Database.user");
		String password = configManager.getConfig().getString("Database.password");
		this.databaseName = configManager.getConfig().getString("Database.database");

		logger.info("{SQL} Connecting to " + type + " Database...");
		
		if (host.equalsIgnoreCase("ipaddress")) {
			
			logger.warning("===================================");
			logger.warning("= Please connect a Database!   =");
			logger.warning("===================================");
			
			throw new Exception("No Database connected.");
		} else {
		
			this.sql = new DatabaseConnector(storage, host, port, user, password, this.databaseName, useSSL);
			this.executor = Executors.newCachedThreadPool();
		}
	}
	
	public void addColumnIfNotExists(String table, String column, String type) {
		
		String checkQuery = "SELECT count(*) AS Counter FROM information_schema.columns WHERE table_schema = '" + this.databaseName + "' and COLUMN_NAME = '" + column + "' AND table_name = '" + table + "' LIMIT 1;";
		String executeQuery = "ALTER TABLE " + this.databaseName + ".`" + table + "` ADD COLUMN `" + column + "` " + type + ";";
		
		this.prepareStatement(checkQuery, new Consumer<ResultSet>() {
			
			@Override
			public void accept(ResultSet rs) {
				try {
					while(rs.next())
						if(rs.getInt("Counter") == 0)
							queryUpdate(executeQuery);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void queryUpdate(PreparedStatement statement) {
		this.executor.execute(() -> this.sql.queryUpdate(statement));
	}

	public void queryUpdate(String statement, String... args) {
		this.executor.execute(() -> sql.queryUpdate(statement, args));
	}
	
	public void prepareStatement(PreparedStatement statement, Consumer<ResultSet> consumer) {
		this.executor.execute(() -> {
			ResultSet result = this.sql.query(statement);
			new Thread(() -> consumer.accept(result)).start();
		});
	}

	public void prepareStatement(String statement, Consumer<ResultSet> consumer) {
		this.executor.execute(() -> {
			ResultSet result = this.sql.query(statement);
			new Thread(() -> consumer.accept(result)).start();
		});
	}
	
	public PreparedStatement prepare(String query) {
		try {
			return this.sql.getConnection().prepareStatement(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DatabaseConnector getMySQL() {
		return this.sql;
	}
}