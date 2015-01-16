package com.csu.powerpdf;

import android.graphics.RectF;
import android.util.Log;

public class Annotation extends RectF {
	enum Type {
		TEXT, LINK, FREETEXT, LINE, SQUARE, CIRCLE, POLYGON, POLYLINE, HIGHLIGHT,
		UNDERLINE, SQUIGGLY, STRIKEOUT, STAMP, CARET, INK, POPUP, FILEATTACHMENT,
		SOUND, MOVIE, WIDGET, SCREEN, PRINTERMARK, TRAPNET, WATERMARK, A3D, UNKNOWN
	}

	public final Type type;
	private float x;
	private float y;
	
	public void setPoint(float left,float top){
		this.x = left;
		this.y = top;
	}
	
	public float getLeft(){
		return this.x;
	}
	
	public float getTop(){
		return this.y;
	}
	
	public Annotation(float x0, float y0, float x1, float y1, int _type) {
		super(x0, y0, x1, y1);
		type = _type == -1 ? Type.UNKNOWN : Type.values()[_type];
	}
}
