package me.truemb.discordnotify.manager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VerifyManager {

    private HashMap<UUID, Long> verifyCache = new HashMap<>(); //CACHES THE AUTHENTICATION PROCESS

    private HashMap<UUID, Long> verifications = new HashMap<>(); //PLAYERS, THAT ARE VERFICATED
    private HashMap<UUID, List<String>> rolesBackup = new HashMap<>(); //ROLES, THAT THE PLAYER HAS
	
	//PLAYER BACKUP ROLES -> To Reset the roles after unlinking
	public void setBackupRoles(UUID uuid, List<String> list) {
		this.rolesBackup.put(uuid, list);
	}

	public void removeBackupRoles(UUID uuid) {
		if(this.rolesBackup.containsKey(uuid))
			this.rolesBackup.remove(uuid);
	}
	
	public List<String> getBackupRoles(UUID uuid) {
		return this.rolesBackup.get(uuid);
	}
	//==================================
	
	
	
	//PLAYER VERIFIED
	public void setVerified(UUID uuid, long disuuid) {
		this.verifications.put(uuid, disuuid);
	}

	public void removeVerified(UUID uuid) {
		if(this.verifications.containsKey(uuid))
			this.verifications.remove(uuid);
	}
	
	public boolean isVerified(UUID uuid) {
		return this.verifications.containsKey(uuid);
	}
	
	public boolean isVerified(long disuuid) {
		return this.verifications.containsValue(disuuid);
	}
	
	public Long getVerfiedWith(UUID uuid) {
		return this.verifications.get(uuid) != null ? this.verifications.get(uuid) : -1;
	}
	
	public UUID getVerfiedWith(long uuid) {
		
		for(UUID mcuuid : this.verifications.keySet()) {
			long disuuid = this.verifications.get(mcuuid);
			if(disuuid == uuid) {
				return mcuuid;
			}
		}
		return null;
	}
	
	//==================================
	
	
	
	//PROGRESS OF THE VERIFICATION
	public void setVerficationProgress(UUID uuid, long disuuid) {
		this.verifyCache.put(uuid, disuuid);
	}
	
	public void clearVerficationProgress(UUID uuid) {
		if(this.verifyCache.containsKey(uuid))
			this.verifyCache.remove(uuid);
	}

	public boolean isVerficationInProgress(UUID uuid) {
		return this.verifyCache.containsKey(uuid);
	}
	
	public Long getVerficationProgress(UUID uuid) {
		return this.verifyCache.get(uuid);
	}

}
