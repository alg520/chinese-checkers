/*
* Copyright (C) 2011- stephane coutant
*
* This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>
*/

package org.scoutant.cc;

import org.scoutant.cc.model.Board;
import org.scoutant.cc.model.Game;
import org.scoutant.cc.model.Move;
import org.scoutant.cc.model.Piece;
import org.scoutant.cc.model.Pixel;
import org.scoutant.cc.model.Player;
import org.scoutant.cc.model.Point;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;


/**
 * 
 * @author scoutant
 * http://commons.wikimedia.org/wiki/Category:Round_icons
 */
public class GameView extends FrameLayout  {
	public static int sizeI = Board.sizeI;
	public static int sizeJ = Board.sizeJ;
	
	private static String tag = "view";
	private static String touch = "touch";
	public int size; 
	public ButtonsView buttons;
	public Piece selected; // ball
	public Point pointed;  // board target point
	public Move move; // current target move in construction
	
	public Game game;
//	public AI ai = new AI(game);
	public static int[] icons = { R.drawable.red, R.drawable.green, R.drawable.pink, R.drawable.blue, R.drawable.violet, R.drawable.orange};
	public Bitmap[] balls = new Bitmap[6];
	public UI ui;
	public SharedPreferences prefs;
	public boolean thinking=false;
	private int dI;
	private int dJ;
	private Bitmap hole ;
	private int diameter;
	private Bitmap iconSelected; 
	private Bitmap iconPointed; 
	private Paint paint = new Paint();
	
	/** In equilateral triangle we have : 1² = (1/2)² + h²  => h = sqrt(3)/2 = 08660254*/
	public GameView(Context context) {
		super(context);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		ui = (UI) context;
		setWillNotDraw(false);
		setLayoutParams( new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.TOP));
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//		Log.i(tag, "width : " + display.getWidth() + ", height : " + display.getHeight());

		setBackgroundResource(R.layout.linear_gradient);
		getBackground().setDither(true);
		
		game = new Game();
		
		dI = display.getWidth()/sizeI;
		dJ = new Double(0.8660254*dI).intValue();
		diameter = new Double( 0.96*dI).intValue();
		Log.i(tag, "width : " + display.getWidth() + ", height : " + display.getHeight() + ", dI : " + dI + ", dJ : " + dJ);
		
		buttons = new ButtonsView(context);
		addView( buttons);
		
		BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        hole = BitmapFactory.decodeResource(context.getResources(), R.drawable.steel);
        iconSelected = BitmapFactory.decodeResource(context.getResources(), R.drawable.ring2);
        iconPointed = BitmapFactory.decodeResource(context.getResources(), R.drawable.ring2);

		paint.setStrokeWidth(0.2f);
		paint.setColor(Color.BLACK);

		for (int color=0; color<6; color++) {
			balls[color] = BitmapFactory.decodeResource(context.getResources(), icons[color]);
		}
	}
	
	public Pixel e;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		if (selected!=null) return false;
		doTouch(event);
		return true;
	}
	
	/** Initialize state so as to accept a fresh new move*/
	public void init() {
		buttons.reset();
		selected = null;
		pointed = null;
		move = null;
		invalidate();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(tag, "KEY : " + keyCode);
		return true;
	}
	
	public void doTouch(MotionEvent event) {
		int action = event.getAction(); 
    	if (action==MotionEvent.ACTION_DOWN) {
    		e = new Pixel( event);
    		Point p = point(e);
    		if (!p.hole()) return;
    		// coordinate of the center of corresponding cell
    		Pixel o = pixel(p);
    		Log.d(touch, "down on " + e + ", p is : " + p + ", center o : " + o);
    		Point n = new Point (p.i, p.j + ( e.y<o.y ? -1 : + 1));
    		// when click in a corner we may be nearer a row up or below 
    		Point s = p;
    		// Yes, 'Neighbor' may actually be closer, if it is a hole it is worth a check
    		if ( n.hole()) {
	    		Pixel oN = pixel(n);
	    		int dO = Pixel.distance(e, o);
	    		int dN = Pixel.distance(e, oN);
	    		Log.d(touch, "neighbour " + n + ", oN " + oN + ", dist O : " + dO + ", dist N : " + dN);
	    		if (dN<dO) Log.i(tag, "Neighboor refining with : " + n);
	    		s = ( dN<dO ? n : p);
    		}
			Log.d(touch, "touched : " + s);
			if (selected==null || (pointed==null && game.ball.is(s))) select( s);
			else point( s);
			invalidate();
    	}
    	if (action==MotionEvent.ACTION_MOVE ) {
    	}
    	if (action==MotionEvent.ACTION_UP ) {
    	}
	}

	
	/** User pretend to select one of his balls */
	private void select(Point p) {
		if (!game.ball.is(p)) return;
		// retrieve ball under selection
		// TODO ensure ball actually is one of his. Or no need and consider case : selected==null...
		selected = game.piece(p);
		Log.d(tag, "selected is now : " + selected);
		buttons.setVisibility(VISIBLE);
	}
	
	/** User pretend to point a free hole as target for next step */
	private void point(Point p) {
		if (game.ball.is(p)) return;
		if (move==null) move = new Move(selected);
		// if user goes back in his move path, we just pop the last 2 points...
		if ( p.equals( move.penultima())) {
			move.pop();
			return;
		}
		move.add(p);
		Log.d(tag, "proposed move length : " + move.points.size());
		pointed = p;
		boolean possible = game.valid( move);
		Log.d(tag, "possible move : " + possible);
		if (!possible) move.pop();
		if (move.points.size()>1) buttons.setOkState( true);
	}
	

	/** @return the cell Point in which Pixel @param l happens to be in */
	private Point point( Pixel l) {
		int j = l.y/dJ;
		// dI/2 offset for odd lines : 
		int oI = (j%2==0 ? 0 : dI/2);
		int i = (l.x-oI)/dI;
		return new Point (i, j);
	}

	/** @return the center of the hole identified by provided @param point */
	private Pixel pixel(Point p) {
		// dI/2 offset for odd lines : 
		int oI = (p.j%2==0 ? 0 : dI/2);
		return new Pixel(dI/2 + p.i*dI+oI, dJ/2 +p.j*dJ);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawBoard(canvas);
	}
	
	private void drawBoard(Canvas canvas){
		for (int j=0; j<=Board.sizeJ; j++){
			canvas.drawLine(0, j*dJ+1, sizeJ*dI, j*dJ+1, paint);
		}
		for (int i=0; i<= sizeI; i++) {
			canvas.drawLine(i*dI, 0, i*dI, sizeI*dJ, paint);
		}
		for (int j=0; j<sizeJ; j++){
			for (int i=0; i< sizeI; i++) {
				Pixel l = pixel(new Point(i, j));
				if (Board.hole.is(i,j)) {
					canvas.drawBitmap(hole, null, toSquare(l, diameter), null);
				}
			}
		}
		if (selected!=null) {
			canvas.drawBitmap( iconSelected, null, toSquare( pixel(selected.point), diameter*12/10), null);
		}
		for (Player player : game.players) {
			for (Piece piece : player.pieces) {
				canvas.drawBitmap( balls[player.color], null, toSquare( pixel( piece.point), diameter*9/10), null);
			}
		}
		
//		if (pointed!=null) {
//			canvas.drawBitmap( iconPointed, null, toSquare( pixel(pointed), diameter/2), null);
//		}
		if (move==null) return;
		for (Point p : move.points) {
			canvas.drawBitmap( iconPointed, null, toSquare( pixel(p), diameter*12/10), null);
			
		}
	}
	
	/**
	 * @return a Rect instance representing a square centered on (@param x, @param y) with @param length  
	 */
	public static Rect toSquare(int x, int y, int length) {
		return new Rect( x-length/2, y-length/2, x+length/2-1, y+length/2-1);
	}
	
	/**
	 * @return a Rect instance representing a square centered on (@param x, @param y) with @param length  
	 */
	public Rect toSquare(Pixel l, int length) {
		return new Rect( l.x-length/2, l.y-length/2, l.x+length/2, l.y+length/2);
	}	
}