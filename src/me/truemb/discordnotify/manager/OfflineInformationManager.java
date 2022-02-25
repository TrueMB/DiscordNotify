package me.truemb.discordnotify.manager;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.discordnotify.enums.InformationType;

public class OfflineInformationManager {
	
    public HashMap<UUID, HashMap<InformationType, String>> offlineDataCacheString = new HashMap<>();
    public HashMap<UUID, HashMap<InformationType, Long>> offlineDataCacheLong = new HashMap<>();
    
	//CACHE
	public void setInformation(UUID uuid, InformationType type, String value) {
		HashMap<InformationType, String> hash = this.offlineDataCacheString.get(uuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		hash.put(type, value);
		
		this.offlineDataCacheString.put(uuid, hash);	
	}
	
	public String getInformationString(UUID uuid, InformationType type) {
		HashMap<InformationType, String> hash = this.offlineDataCacheString.get(uuid);
		
		if(hash == null) 
			return null;
		
		return hash.get(type);
	}

	public void setInformation(UUID uuid, InformationType type, long value) {
		HashMap<InformationType, Long> hash = this.offlineDataCacheLong.get(uuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		hash.put(type, value);
		
		this.offlineDataCacheLong.put(uuid, hash);	
	}
	
	public void addInformation(UUID uuid, InformationType type, long value) {
		HashMap<InformationType, Long> hash = this.offlineDataCacheLong.get(uuid);
		
		if(hash == null) {
			hash = new HashMap<>();
			hash.put(type, value);
		}

		hash.put(type, hash.get(type) + value);
		
		this.offlineDataCacheLong.put(uuid, hash);	
	}
	
	public long getInformationLong(UUID uuid, InformationType type) {
		HashMap<InformationType, Long> hash = this.offlineDataCacheLong.get(uuid);
		
		if(hash == null) 
			return -1;
		
		return hash.get(type) != null ? hash.get(type) : -1;
	}
}
