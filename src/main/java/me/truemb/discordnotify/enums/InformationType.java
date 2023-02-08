package me.truemb.discordnotify.enums;

public enum InformationType {

	Inactivity("VARCHAR(5)"), //IS PLAYER INACTIV?
	IP("VARCHAR(120)"), //PLAYER IP, OF LAST CONNECTION
	Location("VARCHAR(100)"), //LAST POSITION, WHERE THE PLAYER WAS STANDING
	LastConnection("BIGINT"), //LONG - LAST TIME PLAYER WAS SEEN (Only Interesting for Bungeecord)
	Playtime("BIGINT"), //LONG (ticks) - Proxy Connect until Disconnect. Start once on Spigot Server start, all Offlineplayers and get their time.
	Bungee_Server("VARCHAR(80)"); //LAST SERVER, THAT THE PLAYER VISITED

	private String mysqlTypeAsString;
	
	InformationType(String mysqlTypeAsString) {
		this.mysqlTypeAsString = mysqlTypeAsString;
	}
	
	public String getMysqlTypeAsString() {
		return this.mysqlTypeAsString;
	}

}
