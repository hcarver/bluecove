package com.bluecove.emu.gui.graph;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

public class ConnectionEdge extends DefaultEdge {

	private static final long serialVersionUID = 1L;

	private ConnectionSet connectionSet;
	
	public ConnectionEdge(String name, DefaultGraphCell source, DefaultGraphCell target)  {
		super(name);
		attributes = new AttributeMap();

		GraphConstants.setLineBegin(attributes,
				GraphConstants.ARROW_CLASSIC);
		GraphConstants.setBeginSize(attributes, 10);	
		GraphConstants.setBeginFill(attributes, true);
		
		if (GraphConstants.DEFAULTFONT != null) {
			GraphConstants.setFont(attributes, GraphConstants.DEFAULTFONT
					.deriveFont(10));
		}

		GraphConstants.setBendable(attributes,false);
		GraphConstants.setEditable(attributes,false);
		GraphConstants.setSizeable(attributes,false);
		GraphConstants.setDisconnectable(attributes,false);
		

		connectionSet = new ConnectionSet();
		connectionSet.connect(this, source.getChildAt(0), target.getChildAt(0));

	}
	
	public ConnectionSet getConnectionSet() {
		return connectionSet;
	}
}
