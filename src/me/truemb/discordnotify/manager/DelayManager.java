package me.truemb.discordnotify.manager;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.discordnotify.enums.DelayType;

public class DelayManager {

    public HashMap<UUID, HashMap<DelayType, Long>> delayMinecraftHash = new HashMap<>();
    public HashMap<Long, HashMap<DelayType, Long>> delayDiscordHash = new HashMap<>();

	public void setDelay(UUID uuid, DelayType type, long delayTime) {
		HashMap<DelayType, Long> hash = this.delayMinecraftHash.get(uuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		hash.put(type, delayTime);
		
		this.delayMinecraftHash.put(uuid, hash);	
	}
	
	public long getDelay(UUID uuid, DelayType type) {
		
		HashMap<DelayType, Long> hash = this.delayMinecraftHash.get(uuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		return hash.get(type);
	}
	
	public boolean hasDelay(UUID uuid, DelayType type) {
		
		HashMap<DelayType, Long> hash = this.delayMinecraftHash.get(uuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		return hash.get(type) <= System.currentTimeMillis();
	}
	

	public void setDelay(long disuuid, DelayType type, long delayTime) {
		HashMap<DelayType, Long> hash = this.delayDiscordHash.get(disuuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		hash.put(type, delayTime);
		
		this.delayDiscordHash.put(disuuid, hash);	
	}
	
	public long getDelay(long disuuid, DelayType type) {
		
		HashMap<DelayType, Long> hash = this.delayDiscordHash.get(disuuid);
		
		if(hash == null) 
			hash = new HashMap<>();
		
		return hash.get(type) - System.currentTimeMillis();
	}
	
	public boolean hasDelay(long disuuid, DelayType type) {
		
		HashMap<DelayType, Long> hash = this.delayDiscordHash.get(disuuid);
		
		return hash != null && hash.get(type) != null && hash.get(type) > System.currentTimeMillis();
	}
}
