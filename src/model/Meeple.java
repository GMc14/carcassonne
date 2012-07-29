package model;

import java.awt.Color;
import java.awt.Graphics;

import ui.DrawableInterface;

/**
 * @author Andrew Wylie <andrew.dale.wylie@gmail.com>
 * @version 1.0
 * @since 2012-06-08
 */
public class Meeple implements DrawableInterface {

	private Color color;

	// UI variables.
	private int xPos = 0;
	private int yPos = 0;

	public Meeple(Color aColor) {
		// TODO: some other means to differentiate meeple and tile color
		this.color = aColor.brighter().brighter().brighter();
	}

	public Color getColor() {
		return this.color;
	}

	public int getx() {
		return xPos;
	}

	public int gety() {
		return yPos;
	}

	public void getx(int tilex) {
		this.xPos = tilex;
	}

	public void gety(int tiley) {
		this.yPos = tiley;
	}

	@Override
	public void draw(Graphics g) {

		// Create some sort of a shape to be on the middle of a tile.
		g.setColor(color);
		g.fillRoundRect(xPos, yPos, Tile.tileTypeSize, Tile.tileTypeSize,
				Tile.tileTypeSize / 4, Tile.tileTypeSize / 4);

	}

}
