package me.truemb.discordnotify.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

import me.truemb.discordnotify.database.connector.AsyncMySQL;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.manager.OfflineInformationManager;

public class OfflineInformationsSQL {
	
	private AsyncMySQL asyncMysql;
	private OfflineInformationManager offlineInfoManager;
	public static final String table = "disnotify_offlineData"; //Also hardcoded in BC_InactivityChecker
	
	public OfflineInformationsSQL(AsyncMySQL asyncMysql, OfflineInformationManager offlineInfoManager){
		this.asyncMysql = asyncMysql;
		this.offlineInfoManager = offlineInfoManager;

		this.asyncMysql.queryUpdate("CREATE TABLE IF NOT EXISTS " + OfflineInformationsSQL.table + " (uuid VARCHAR(60) PRIMARY KEY, ingamename VARCHAR(18))");
		this.checkColumnsForUpdates();
	}

	public void checkColumnsForUpdates(){
		this.asyncMysql.addColumnIfNotExists(OfflineInformationsSQL.table, "ingamename", "VARCHAR(16)"); //WASN'T IMPLEMENTED IN THE FIRST VERSIONS OF DISCORDNOTIFY
		
		for(InformationType types : InformationType.values())
			this.asyncMysql.addColumnIfNotExists(OfflineInformationsSQL.table, types.toString(),types.getMysqlTypeAsString());
	}
	
	public void checkForNameChange(UUID uuid, String ingameName) {
		this.asyncMysql.queryUpdate("INSERT INTO " + OfflineInformationsSQL.table + " (uuid, ingamename) VALUES (?, ?) "
				+ "ON DUPLICATE KEY UPDATE ingamename=?;",
			uuid.toString(), ingameName, ingameName);
	}
	
	public void updateInformation(UUID uuid, InformationType type, String value){
		this.asyncMysql.queryUpdate("INSERT INTO " + OfflineInformationsSQL.table + " (uuid, " + type.toString() + ") VALUES (?,?) "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "=?;",
			uuid.toString(), value, value);
	}
	
	public void updateInformation(UUID uuid, InformationType type, long value){
		this.asyncMysql.queryUpdate("INSERT INTO " + OfflineInformationsSQL.table + " (uuid, " + type.toString() + ") VALUES (?,?) "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "=?;",
			uuid.toString(), String.valueOf(value), String.valueOf(value));
	}
	
	/**
	 * Used to add f.e. the playtime
	 * 
	 * @param uuid - Player Identicator
	 * @param type - InformationType
	 * @param value - Value of the Type
	 */
	public void addToInformation(UUID uuid, InformationType type, long value){
		this.asyncMysql.queryUpdate("INSERT INTO " + OfflineInformationsSQL.table + " (uuid, " + type.toString() + ") VALUES (?,?) "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "=IFNULL(" + type.toString() + ", 0) + ?;",
			uuid.toString(), String.valueOf(value), String.valueOf(value));
	}
	
	public void setup() {
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + OfflineInformationsSQL.table + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						
						UUID uuid = UUID.fromString(rs.getString("uuid"));
						
						for(InformationType types : InformationType.values()) {
							if(types.getMysqlTypeAsString().contains("VARCHAR")) {
								
								String value = rs.getString(types.toString());
								offlineInfoManager.setInformation(uuid, types, value);
								
							}else if(types.getMysqlTypeAsString().contains("BIGINT")) {
									
								long value = rs.getLong(types.toString());
								offlineInfoManager.setInformation(uuid, types, value);
									
							}
						}
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public OfflineInformationManager getOfflineInfoManager() {
		return this.offlineInfoManager;
	}
}
