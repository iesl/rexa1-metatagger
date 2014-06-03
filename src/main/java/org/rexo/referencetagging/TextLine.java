/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Feb 17, 2005
 * author: saunders
 */

package org.rexo.referencetagging;

import java.awt.geom.Point2D;


public class TextLine implements MyTextObject {
	public Point2D.Double getCoord() {
		return coord;
	}

	Point2D.Double coord = new Point2D.Double();

	public int getPage() {
		return _page;
	}

	public void setPage(int page) {
		_page = page;
	}

	private int _page = 0;

	//int page;
	//List contentList;
	long startTokenNum = 0;
	long endTokenNum = 0;

	String text;

	public TextLine(long startTokenNum, long endTokenNum, int x, int y, int page, String text) {
		this.startTokenNum = startTokenNum;
		this.endTokenNum = endTokenNum;
		this.coord.x = x;
		this.coord.y = y;
		this.text = text;
		this._page = page;
	}

	public String getText() {
		return text;
	}

}
