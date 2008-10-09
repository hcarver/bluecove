package com.bluecove.emu.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.jgraph.graph.GraphLayoutCache;

import com.bluecove.emu.gui.graph.GraphView;
import com.bluecove.emu.gui.graph.GraphPane;

public class EmulatorPane extends JPanel {

	private static final long serialVersionUID = 1L;

	private GraphPane graphPane;

	private GraphView graphView;

	private JPanel detailsPane;

	private JPanel connectionsPane;

	public EmulatorPane() {
		super();
		setLayout(new BorderLayout());
		
		graphView = new GraphView();
		
		graphPane = new GraphPane(graphView);
		detailsPane = new JPanel();
		detailsPane.add(new JLabel("DETAILS"));
		connectionsPane = new JPanel();
		connectionsPane.add(new JLabel("CONNECTIONS"));
		
		
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new BorderLayout());
		
		JSplitPane horisontalSplit = createSplitPane(
				new JScrollPane(graphPane), detailsPane, JSplitPane.HORIZONTAL_SPLIT);
		upperPanel.add(horisontalSplit, BorderLayout.CENTER);
		
		JSplitPane verticalSplit = createSplitPane(
				upperPanel, connectionsPane, JSplitPane.VERTICAL_SPLIT);
		add(verticalSplit);
	}

	
	
	public JSplitPane createSplitPane(Component first, Component second,
			int orientation) {
		JSplitPane splitPane = new JSplitPane(orientation, first, second);
		splitPane.setBorder(null);
		splitPane.setFocusable(false);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.2);
		return splitPane;
	}

}
