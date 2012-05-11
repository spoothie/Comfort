package net.spoothie.comfort;

import java.util.ArrayList;

import net.minecraft.server.DataWatcher;
import net.minecraft.server.WatchableObject;

public class ComfortDataWatcher extends DataWatcher {

	private byte metadata;
	
	public ComfortDataWatcher(byte metadata) {
		this.metadata = metadata;
	}
	
	@Override
	public ArrayList<WatchableObject> b() {
		ArrayList<WatchableObject> list = new ArrayList<WatchableObject>();
		WatchableObject wo = new WatchableObject(0, 0, metadata);
		list.add(wo);
		return list;
	}
	
}
