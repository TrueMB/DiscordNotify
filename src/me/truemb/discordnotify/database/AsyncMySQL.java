package me.truemb.discordnotify.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import me.truemb.discordnotify.manager.ConfigManager;

public class AsyncMySQL {
	
	//https://www.youtube.com/watch?v=dHjp0pRhGhk
	
	private ExecutorService executor;
	private MySQL sql;
	
	private String databaseName;

	public AsyncMySQL(Logger logger, ConfigManager configManager) throws Exception {
				
		String host = configManager.getConfig().getString("Database.host");
		int port = configManager.getConfig().getInt("Database.port");
		boolean useSSL = configManager.getConfig().getBoolean("Database.useSSL");
		String user = configManager.getConfig().getString("Database.user");
		String password = configManager.getConfig().getString("Database.password");
		this.databaseName = configManager.getConfig().getString("Database.database");
		
		if (host.equalsIgnoreCase("ipaddress")) {
			
			logger.warning("===================================");
			logger.warning("= Please connect a Database!   =");
			logger.warning("===================================");
			
			throw new Exception("No Database connected.");
		} else {
		
			this.sql = new MySQL(host, port, user, password, this.databaseName, useSSL);
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

	public void queryUpdate(String statement) {
		this.executor.execute(() -> sql.queryUpdate(statement));
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

	public MySQL getMySQL() {
		return this.sql;
	}

	public static class MySQL {

		private String host, user, password, database;
		private int port;
		private boolean useSSL;

		private Connection conn;

		public MySQL(String host, int port, String user, String password, String database, boolean useSSL) throws Exception {
			this.host = host;
			this.port = port;
			this.user = user;
			this.password = password;
			this.database = database;
			this.useSSL = useSSL;

			this.openConnection();
		}

		public void queryUpdate(String query) {
			checkConnection();
			try (PreparedStatement statement = conn.prepareStatement(query)) {
				queryUpdate(statement);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void queryUpdate(PreparedStatement statement) {
			checkConnection();
			try {
				statement.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					statement.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public ResultSet query(String query) {
			checkConnection();
			try {
				return query(conn.prepareStatement(query));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public ResultSet query(PreparedStatement statement) {
			checkConnection();
			try {
				return statement.executeQuery();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public Connection getConnection() {
			return this.conn;
		}

		public void checkConnection() {
			try {
				if (this.conn == null || !this.conn.isValid(10) || this.conn.isClosed())
					openConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Connection openConnection() throws Exception {
	        Class.forName("com.mysql.cj.jdbc.Driver");
			return this.conn = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useSSL=" + String.valueOf(this.useSSL), this.user, this.password);
		}
		
		public void closeRessources(ResultSet rs, PreparedStatement st){
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {}
			}
			if(st != null){
				try {
					st.close();
				} catch (SQLException e) {}
			}
		}

		public void closeConnection() {
			try {
				this.conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				this.conn = null;
			}
		}
	}
}