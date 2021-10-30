package me.truemb.disnotify.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.manager.OfflineInformationManager;

public class OfflineInformationsSQL {
	
	private AsyncMySQL asyncMysql;
	private OfflineInformationManager offlineInfoManager;
	private String table = "disnotify_offlineData"; //Also hardcoded in BC_InactivityChecker
	//OLD OfflineInfo table
	
	public OfflineInformationsSQL(AsyncMySQL asyncMysql, OfflineInformationManager offlineInfoManager){
		this.asyncMysql = asyncMysql;
		this.offlineInfoManager = offlineInfoManager;

		//OLD this.asyncMysql.queryUpdate("CREATE TABLE IF NOT EXISTS " + this.table + " (uuid VARCHAR(60), type VARCHAR(100), value VARCHAR(100), PRIMARY KEY(uuid, type))");
		this.asyncMysql.queryUpdate("CREATE TABLE IF NOT EXISTS " + this.table + " (uuid VARCHAR(60) PRIMARY KEY)");
		this.checkColumnsForUpdates();
	}

	public void checkColumnsForUpdates(){
		String columns = "";
		for(InformationType types : InformationType.values())
			columns += ", " + types.toString() + " " + types.getMysqlTypeAsString();
		columns = columns.substring(2, columns.length());
		
		this.asyncMysql.queryUpdate("ALTER TABLE " + this.table + " ADD COLUMN IF NOT EXISTS (" + columns + ");");
	}
	
	
	public void updateInformation(UUID uuid, InformationType type, String value){
		this.asyncMysql.queryUpdate("INSERT INTO " + this.table + " (uuid, " + type.toString() + ") VALUES ('" + uuid.toString() + "', '" + value + "') "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "='" + value + "';");
	}
	
	public void updateInformation(UUID uuid, InformationType type, long value){
		this.asyncMysql.queryUpdate("INSERT INTO " + this.table + " (uuid, " + type.toString() + ") VALUES ('" + uuid.toString() + "', '" + value + "') "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "='" + value + "';");
	}
	
	/**
	 * Used to add f.e. the playtime
	 * 
	 * @param uuid - Player Identicator
	 * @param type - InformationType
	 * @param value - Value of the Type
	 */
	public void addToInformation(UUID uuid, InformationType type, long value){
		this.asyncMysql.queryUpdate("INSERT INTO " + this.table + " (uuid, " + type.toString() + ") VALUES ('" + uuid.toString() + "', '" + value + "') "
				+ "ON DUPLICATE KEY UPDATE " + type.toString() + "=" + type.toString() + " + '" + value + "';");
	}
	
	public void setup() {
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + this.table + ";", new Consumer<ResultSet>() {

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

	//NOT NEEDED ANYMORE, SEND SQL DATA AND ONLY PLUGIN MESSAGING FOR CHANGES
	
	//COULD BE USED FOR SERVER CHANGE EVENT
	//Spigot Server saves on Disconnect
	//Bungeecord could update on server join to refresh data
	//
	//Whats happens if player only disconnects? Data needs to be refreshed. Is there a server change type and disconnect type? Can you see the destination? Is it maybe null?
	/*
	public void updatePlayer(UUID uuid) {
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + this.table + " WHERE uuid='" + uuid.toString() + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {

						UUID uuid = UUID.fromString(rs.getString("uuid"));
						InformationType type = InformationType.valueOf(rs.getString("type"));
						String value = rs.getString("value");
						
						offlineInfoManager.setInformation(uuid, type, value);
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	*/
}
