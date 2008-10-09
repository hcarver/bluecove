package com.bluecove.emu.gui.model;

import java.util.Observable;

public class Datum extends Observable {

	private long id;
	
	public Datum(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return "Datum[id="+id+"]";
	}

}
