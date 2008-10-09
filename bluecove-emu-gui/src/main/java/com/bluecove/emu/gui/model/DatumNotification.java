package com.bluecove.emu.gui.model;

public class DatumNotification {

	public static enum Type {REMOVED, ADDED, CHANGED};
	
	private Type type;
	private Datum datum;
	
	public DatumNotification(Type type, Datum datum) {
		this.type = type;
		this.datum = datum;
	}

	public Type getType() {
		return type;
	}

	public Datum getDatum() {
		return datum;
	}
	
	@Override
	public String toString() {
		return "DatumNotification[type="+type+";datum="+datum+"]";
	}
}
