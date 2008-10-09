package com.bluecove.emu.gui.graph;

import org.jgraph.JGraph;

public class GraphPane extends JGraph {

	private static final long serialVersionUID = 1L;

	public static final int PANE_HEIGHT = 400;
	public static final int PANE_WIDTH = 400;

	
	public GraphPane(GraphView view) {
		super(view);
		
		setSize(PANE_WIDTH, PANE_HEIGHT);
		
		view.addStaff();
		
	}

	
}
