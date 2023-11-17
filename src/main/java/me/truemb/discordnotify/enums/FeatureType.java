package me.truemb.discordnotify.enums;

public enum FeatureType {
	
	Inactivity(false),
	PlayerJoinLeave(false),
	PlayerDeath(false),
	PlayerAdvancement(false),
	Chat(false),
	Staff(false),
	RoleSync(false),
	ServerStatus(false),
	
	//ADDONS
	Verification(true),
	PlayerInfo(true);

	
	private boolean isAddon;
	
	FeatureType(boolean isAddon) {
		this.isAddon = isAddon;
	}

	public boolean isAddon() {
		return this.isAddon;
	}

}
