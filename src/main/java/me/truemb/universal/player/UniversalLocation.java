package me.truemb.universal.player;

import lombok.Getter;

@Getter
public class UniversalLocation {
	
	private String worldname;
	
	private double x;
	private double y;
	private double z;

	private double yaw;
	private double pitch;

	public UniversalLocation(String worldname, double x, double y, double z, double yaw, double pitch) {
		this(worldname, x, y, z);
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public UniversalLocation(String worldname, double x, double y, double z) {
		this.worldname = worldname;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getBlockX() {
		return (int) this.x;
	}
	
	public int getBlockY() {
		return (int) this.y;
	}
	
	public int getBlockZ() {
		return (int) this.z;
	}

}
