package com.bluecove.emu.gui.graph;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import com.bluecove.emu.gui.BluecoveEmulatorUI;
import com.bluecove.emu.gui.model.Device;

public class DeviceCell extends DefaultGraphCell {

	private static final long serialVersionUID = 1L;

	private Device device;
	
	private static ArrayList<Spot> spots = new ArrayList<Spot>();
	
	private static double positionRadians[] = {0, Math.PI, Math.PI/2, Math.PI*3/2, Math.PI/4, Math.PI*5/4, Math.PI*3/4, Math.PI*7/4};
	
	public DeviceCell(Device device)  {
		super(" " + device.getId());
		this.device = device;
		attributes = new AttributeMap();
		addPort(null, "JGraph/Center");
		GraphConstants.setVerticalTextPosition (attributes, SwingConstants.BOTTOM);
		
		try {
			ImageIcon icon = new ImageIcon(ImageIO.read(BluecoveEmulatorUI.class
					.getResource("/images/phone.png")));
		    GraphConstants.setIcon(attributes, icon);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		GraphConstants.setOpaque(attributes, false);
		GraphConstants.setBendable(attributes,false);
		GraphConstants.setEditable(attributes,false);
		GraphConstants.setSizeable(attributes,false); 
		GraphConstants.setAutoSize(attributes, true);
		
		DeviceCellViewFactory.setViewClass(attributes, DeviceCellView.class.getCanonicalName());
	}
	
	public void beforeInsert() {
		GraphConstants.setBounds(attributes, allocateSpot(this).getBounds());
	}

	public void afterRemove() {
		deallocateSpot(this);
	}

	private static synchronized Spot allocateSpot(DeviceCell device) {
		for (int i = 0; i < spots.size(); i++) {
			Spot spot = spots.get(i);
			if(spot.getDevice() == null) {
				spot.setDevice(device);
				return spot;
			}
		}
		
		Spot spot = new Spot(calculateSpotBounds(spots.size()) ,device);
		spots.add(spot);
		return spot;
	}
	
	private static Rectangle calculateSpotBounds(int index) {
		int x = 0;
		int y = 0;
		if (index < 8) {
			x = (int)(GraphPane.PANE_WIDTH/2 - Math.cos(positionRadians[index])*(GraphPane.PANE_WIDTH/2-10));
			y = (int)(GraphPane.PANE_HEIGHT/2 - Math.sin(positionRadians[index])*(GraphPane.PANE_HEIGHT/2-10));
		} else {
			throw new Error("Max 8 devices is currently supported.");
		}
    	return new Rectangle(x, y ,DeviceCellView.IMAGE_WIDTH, DeviceCellView.IMAGE_HEIGHT); 
    } 
	
	private static synchronized void deallocateSpot(DeviceCell device) {
		for (int i = 0; i < spots.size(); i++) {
			Spot spot = spots.get(i);
			if(spot.getDevice().equals(device)) {
				spot.setDevice(null);
				return;
			}
		}

	}
	
	private static class Spot {
		
		Rectangle2D bounds;
		DeviceCell device;
		
		public Spot(Rectangle2D bounds, DeviceCell device) {
			super();
			this.bounds = bounds;
			this.device = device;
		}

		public DeviceCell getDevice() {
			return device;
		}

		public void setDevice(DeviceCell device) {
			this.device = device;
		}

		public Rectangle2D getBounds() {
			return bounds;
		}
		
	}

	public Device getDevice() {
		return device;
	}
}
