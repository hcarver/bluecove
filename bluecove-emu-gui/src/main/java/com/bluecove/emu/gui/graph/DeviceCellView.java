package com.bluecove.emu.gui.graph;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexRenderer;
import org.jgraph.graph.VertexView;

public class DeviceCellView extends VertexView {

	public static transient JGraphEllipseRenderer renderer = new JGraphEllipseRenderer();

	public static final int IMAGE_HEIGHT = 48;
	public static final int IMAGE_WIDTH = 48;
	private static final int IMAGE_VERTICAL_OFFSET = 16;
	
	public DeviceCellView() {
		super();
	}

	public DeviceCellView(Object cell) {
		super(cell);
	}

	private Rectangle2D getImageBounds() {
		Rectangle2D original = getBounds();
		int x = (int)(original.getX() + (original.getWidth() - IMAGE_WIDTH)/2);
		int y = (int)(original.getY()) + IMAGE_VERTICAL_OFFSET;
		return new Rectangle(x, y, IMAGE_WIDTH, IMAGE_HEIGHT);
	}
	
	public Point2D getPerimeterPoint(EdgeView edge, Point2D source, Point2D p) {
		Rectangle2D r = getImageBounds();

		double x = r.getX();
		double y = r.getY();
		double a = (r.getWidth() + 1) / 2;
		double b = (r.getHeight() + 1) / 2;

		// x0,y0 - center of ellipse
		double x0 = x + a;
		double y0 = y + b;

		// x1, y1 - point
		double x1 = p.getX();
		double y1 = p.getY();

		// calculate straight line equation through point and ellipse center
		// y = d * x + h
		double dx = x1 - x0;
		double dy = y1 - y0;

		if (dx == 0)
			return new Point((int) x0, (int) (y0 + b * dy / Math.abs(dy)));

		double d = dy / dx;
		double h = y0 - d * x0;

		// calculate intersection
		double e = a * a * d * d + b * b;
		double f = -2 * x0 * e;
		double g = a * a * d * d * x0 * x0 + b * b * x0 * x0 - a * a * b * b;

		double det = Math.sqrt(f * f - 4 * e * g);

		// two solutions (perimeter points)
		double xout1 = (-f + det) / (2 * e);
		double xout2 = (-f - det) / (2 * e);
		double yout1 = d * xout1 + h;
		double yout2 = d * xout2 + h;

		double dist1Squared = Math.pow((xout1 - x1), 2)
				+ Math.pow((yout1 - y1), 2);
		double dist2Squared = Math.pow((xout2 - x1), 2)
				+ Math.pow((yout2 - y1), 2);

		// correct solution
		double xout, yout;

		if (dist1Squared < dist2Squared) {
			xout = xout1;
			yout = yout1;
		} else {
			xout = xout2;
			yout = yout2;
		}

		return getAttributes().createPoint(xout, yout);
	}

	/**
	 */
	public CellViewRenderer getRenderer() {
		return renderer;
	}

	/**
	 */
	public static class JGraphEllipseRenderer extends VertexRenderer {

		/**
		 * Return a slightly larger preferred size than for a rectangle.
		 */
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			d.width += d.width / 8;
			d.height += d.height / 2;
			return d;
		}

		/**
		 */
		public void paint(Graphics g) {
			int b = borderWidth;
			Graphics2D g2 = (Graphics2D) g;
			
			int x = (int)((getSize().getWidth() - IMAGE_WIDTH)/2);
			int y = IMAGE_VERTICAL_OFFSET;
		
			boolean tmp = selected;
			if (super.isOpaque()) {
				g.setColor(super.getBackground());
				if (gradientColor != null && !preview) {
					setOpaque(false);
					g2.setPaint(new GradientPaint(0, 0, getBackground(),
							getWidth(), getHeight(), gradientColor, true));
				}
				g.fillOval(x + b - 1, y + b - 1, IMAGE_WIDTH - b, IMAGE_HEIGHT - b);
			}
			try {
				setBorder(null);
				setOpaque(false);
				selected = false;
				super.paint(g);
			} finally {
				selected = tmp;
			}
			if (bordercolor != null) {
				g.setColor(bordercolor);
				g2.setStroke(new BasicStroke(b));
				g.drawOval(x + b - 1, y + b - 1, IMAGE_WIDTH - b, IMAGE_HEIGHT - b);
			}
			if (selected) {
				g2.setStroke(GraphConstants.SELECTION_STROKE);
				g.setColor(highlightColor);
				g.drawOval(x + b - 1, y + b - 1, IMAGE_WIDTH - b, IMAGE_HEIGHT - b);
			}
		}
	}
}