package com.gpl.rpg.AndorsTrail.view;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.context.ViewContext;
import com.gpl.rpg.AndorsTrail.controller.EffectController.EffectAnimation;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.LayeredWorldMap;
import com.gpl.rpg.AndorsTrail.model.map.MapLayer;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.resource.TileStore;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.Size;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public final class MainView extends SurfaceView implements SurfaceHolder.Callback {

    private int displayTileSize = 32;

    private Size screenSizeTileCount = null;
    private final Coord screenOffset = new Coord(); // pixel offset where the image begins
    private final Coord mapTopLeft = new Coord(); // Map coords of visible map
    private CoordRect mapViewArea; // Area in mapcoordinates containing the visible map. topleft == this.topleft
    
    private final ModelContainer model;
    private final TileStore tiles;
	private final ViewContext view;
	
    private final SurfaceHolder holder;
    private final Paint mPaint = new Paint();
	private final CoordRect p1x1 = new CoordRect(new Coord(), new Size(1,1));
    private Bitmap doubleBuffer; 
    private Canvas doubleBufferCanvas; 
	private boolean hasSurface = false;

    private final Coord lastTouchPosition = new Coord();

	public MainView(Context context, AttributeSet attr) {
		super(context, attr);
		this.holder = getHolder();
		
    	AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivityContext(context);
        this.view = app.currentView.get();
        this.model = app.world.model;
    	this.tiles = app.world.tileStore;
    	
    	holder.addCallback(this);
    	
        setFocusable(true);
        requestFocus();
        setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainView.this.onClick();
			}
		});
        setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return MainView.this.onLongClick();
			}
		});
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
    	if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
    		movePlayer(0, -1);
    	} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
    		movePlayer(0, 1);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	movePlayer(-1, 0);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	movePlayer(1, 0);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        	this.onClick();
        } else {
        	return super.onKeyDown(keyCode, msg);
        }
    	//TODO: add more keys
    	return true;
    }

	private void movePlayer(int dx, int dy) {
		view.movementController.movePlayer(dx, dy);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (w <= 0 || h <= 0) return;

		L.log("surfaceChanged " + w + ", " + h);

		displayTileSize = tiles.displayTileSize;
		
		screenSizeTileCount = new Size(
				(int) Math.floor(w / displayTileSize)
				,(int) Math.floor(h / displayTileSize)
			);

    	screenOffset.set(
				(w - (displayTileSize * screenSizeTileCount.width)) / 2
				,(h - (displayTileSize * screenSizeTileCount.height)) / 2
			);
    	
		if (doubleBuffer != null) {
    		doubleBuffer.recycle();
    	}
		doubleBuffer = Bitmap.createBitmap(displayTileSize * screenSizeTileCount.width, displayTileSize * screenSizeTileCount.height, Bitmap.Config.RGB_565);
    	doubleBufferCanvas = new Canvas(doubleBuffer);
    	//doubleBufferCanvas.clipRect(0, 0, displayTileSize * screenSizeTileCount.width, displayTileSize * screenSizeTileCount.height);
	    	
    	if (model.currentMap != null) {
    		notifyMapChanged();
    	}
    	
    	redrawAll();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		hasSurface = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
		L.log("surfaceDestroyed");
		if (doubleBuffer != null) {
    		doubleBufferCanvas = null;
    		doubleBuffer.recycle();
    		doubleBuffer = null;
    	}
	}

    private void onClick() {
    	if (model.uiSelections.isInCombat) {
			view.combatController.executeMoveAttack();
		}
    }
    
    private boolean onLongClick() {
    	final Coord tilePosition = getScreenToTilePosition(lastTouchPosition);
		final int dx = tilePosition.x - model.player.position.x;
		final int dy = tilePosition.y - model.player.position.y;
		if (model.uiSelections.isInCombat) {
			//TODO: Should be able to mark positions far away (mapwalk / ranged combat)
			if (dx == 0 && dy == 0) return false;
			if (Math.abs(dx) > 1) return false;
			if (Math.abs(dy) > 1) return false;
				
			view.combatController.setCombatSelection(tilePosition);
			return true;
		}
		return false;
    }
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			lastTouchPosition.set((int)event.getX(), (int)event.getY());
			if (!model.uiSelections.isInCombat) {
				final Coord tilePosition = getScreenToTilePosition(lastTouchPosition);
				final int dx = tilePosition.x - model.player.position.x;
				final int dy = tilePosition.y - model.player.position.y;
				view.movementController.movePlayer(sgn(dx), sgn(dy));
				return true;
			}
		}
		return super.onTouchEvent(event);
	}
	
	private static int sgn(final int v) { 
		if (v == 0) return 0;
		else if (v > 0) return 1;
		else return -1;
	}
    
    private Coord getScreenToTilePosition(Coord c) {
    	return new Coord(
				(int) Math.floor((c.x - screenOffset.x) / displayTileSize) + mapTopLeft.x
				,(int) Math.floor((c.y - screenOffset.y) / displayTileSize) + mapTopLeft.y
				);
	}

	public void redrawAll() {
		updateDoubleBuffer(mapViewArea);
		redrawFromDoubleBuffer();
	}
	
	public void redrawTile(final Coord p) {
		p1x1.topLeft.set(p);
		updateDoubleBuffer(p1x1);
		redrawFromDoubleBuffer();
	}
	private void updateDoubleBuffer(final CoordRect area) {
		if (!hasSurface) return;
		doDrawRect(doubleBufferCanvas, area);
	}
	public void redrawFromDoubleBuffer() {
		if (!hasSurface) return;
		
		//L.log("Redraw ");
		//long start = System.currentTimeMillis();
		
		Canvas c = null;
		try {
	        c = holder.lockCanvas(null);
	        synchronized (holder) {
	        	c.drawBitmap(doubleBuffer, screenOffset.x, screenOffset.y, mPaint);
	        }
	    } finally {
	        // do this in a finally so that if an exception is thrown
	        // during the above, we don't leave the Surface in an
	        // inconsistent state
	        if (c != null) {
	        	holder.unlockCanvasAndPost(c);
	        }
	    }
	    //long stop = System.currentTimeMillis();
		//L.log("draw: " + (stop-start) + "ms");
	}
	
	public void redrawTileWithEffect(final CoordRect area, final EffectAnimation effect) {
		if (!hasSurface) return;
		
		Canvas c = null;
		try {
	        c = holder.lockCanvas(null);
	        synchronized (holder) {
	        	c.translate(screenOffset.x, screenOffset.y);
	        	c.drawBitmap(doubleBuffer, 0, 0, mPaint);
	        	drawFromMapPosition(c, area, effect.position.x, effect.position.y, effect.currentTileID);
    			if (effect.displayText != null) {
    				drawEffectText(c, area, effect);
    			}
	        }
	    } finally {
	        // do this in a finally so that if an exception is thrown
	        // during the above, we don't leave the Surface in an
	        // inconsistent state
	        if (c != null) {
	        	holder.unlockCanvasAndPost(c);
	        }
	    }
	}
	
	
	/*
	private void doDraw(Canvas canvas) {
    	final LayeredWorldMap currentMap = model.currentMap;
        
        drawMapLayer(canvas, currentMap.layers[LayeredWorldMap.LAYER_GROUND]);
        tryDrawMapLayer(canvas, currentMap, LayeredWorldMap.LAYER_OBJECTS);
        
        for (Loot l : currentMap.groundBags) {
			drawFromMapPosition(canvas, l.position, TileStore.iconID_groundbag);
		}
        
		drawFromMapPosition(canvas, model.player.position, model.player.traits.iconID);
		for (MonsterSpawnArea a : currentMap.spawnAreas) {
			for (Monster m : a.monsters) {
				drawFromMapPosition(canvas, m.position, m.traits.iconID);
			}
		}
		
		tryDrawMapLayer(canvas, currentMap, LayeredWorldMap.LAYER_ABOVE);
        
		if (model.uiSelections.selectedMonster != null) {
			drawFromMapPosition(canvas, model.uiSelections.selectedPosition, TileStore.iconID_attackselect);
		} else if (model.uiSelections.selectedPosition != null) {
			drawFromMapPosition(canvas, model.uiSelections.selectedPosition, TileStore.iconID_moveselect);
		}
		
		for (EffectAnimation e : view.effectController.currentEffects) {
			if (e == null) continue;
			drawFromMapPosition(canvas, e.position.x, e.position.y, e.currentTileID);
			if (e.displayText != null) {
				drawEffectText(canvas, e);
			}
		}
    }
	
	private void doDrawTile(final Canvas canvas, final Coord pos) {
		int sx = pos.x - mapTopLeft.x;
		int sy = pos.y - mapTopLeft.y;
		if (sx < 0 || sx > mapViewSize.width) return;
		if (sy < 0 || sy > mapViewSize.height) return;
		sx *= mTileSize;
		sy *= mTileSize;
		
		final int mx = pos.x;
		final int my = pos.y;
		
		final LayeredWorldMap currentMap = model.currentMap;
        int tile = currentMap.layers[LayeredWorldMap.LAYER_GROUND].tiles[mx][my];
		if (tile != 0) canvas.drawBitmap(tiles.bitmaps[tile], sx, sy, mPaint);
		
		tile = currentMap.layers[LayeredWorldMap.LAYER_OBJECTS].tiles[mx][my];
		if (tile != 0) canvas.drawBitmap(tiles.bitmaps[tile], sx, sy, mPaint);
		
        for (Loot l : currentMap.groundBags) {
        	if (l.position.equals(pos)) {
        		canvas.drawBitmap(tiles.bitmaps[TileStore.iconID_groundbag], sx, sy, mPaint);
        	}
		}
        
        if (model.player.position.equals(pos)) {
        	canvas.drawBitmap(tiles.bitmaps[model.player.traits.iconID], sx, sy, mPaint);
        }
		
    	for (MonsterSpawnArea a : currentMap.spawnAreas) {
			for (Monster m : a.monsters) {
				if (m.position.equals(pos)) {
	        		canvas.drawBitmap(tiles.bitmaps[m.traits.iconID], sx, sy, mPaint);
	        	}
			}
		}

    	if (currentMap.layers.length > LayeredWorldMap.LAYER_ABOVE) {
    		tile = currentMap.layers[LayeredWorldMap.LAYER_ABOVE].tiles[mx][my];
    		if (tile != 0) canvas.drawBitmap(tiles.bitmaps[tile], sx, sy, mPaint);
    	}
        
    	if (model.uiSelections.selectedPosition != null && model.uiSelections.selectedPosition.equals(pos)) {
			if (model.uiSelections.selectedMonster != null) {
				canvas.drawBitmap(tiles.bitmaps[TileStore.iconID_attackselect], sx, sy, mPaint);
			} else if (model.uiSelections.selectedPosition != null) {
				canvas.drawBitmap(tiles.bitmaps[TileStore.iconID_moveselect], sx, sy, mPaint);
			}
    	}
		
		for (EffectAnimation e : view.effectController.currentEffects) {
			if (e == null) continue;
			if (e.position.equals(pos)) {
				canvas.drawBitmap(tiles.bitmaps[e.currentTileID], sx, sy, mPaint);
				if (e.displayText != null) {
					drawEffectText(canvas, e);
				}
			}
		}
    }
	*/
	
	private void doDrawRect(Canvas canvas, CoordRect area) {
    	final LayeredWorldMap currentMap = model.currentMap;
        
        drawMapLayer(canvas, area, currentMap.layers[LayeredWorldMap.LAYER_GROUND]);
        tryDrawMapLayer(canvas, area, currentMap, LayeredWorldMap.LAYER_OBJECTS);
        
        for (Loot l : currentMap.groundBags) {
			drawFromMapPosition(canvas, area, l.position, TileStore.iconID_groundbag);
		}
        
		drawFromMapPosition(canvas, area, model.player.position, model.player.traits.iconID);
		for (MonsterSpawnArea a : currentMap.spawnAreas) {
			for (Monster m : a.monsters) {
				drawFromMapPosition(canvas, area, m.position, m.traits.iconID);
			}
		}
		
		tryDrawMapLayer(canvas, area, currentMap, LayeredWorldMap.LAYER_ABOVE);
        
		if (model.uiSelections.selectedPosition != null) {
			if (model.uiSelections.selectedMonster != null) {
				drawFromMapPosition(canvas, area, model.uiSelections.selectedPosition, TileStore.iconID_attackselect);
			} else {
				drawFromMapPosition(canvas, area, model.uiSelections.selectedPosition, TileStore.iconID_moveselect);
			}
		}
    }
    
	private void tryDrawMapLayer(Canvas canvas, final CoordRect area, final LayeredWorldMap currentMap, final int layerIndex) {
    	if (currentMap.layers.length > layerIndex) drawMapLayer(canvas, area, currentMap.layers[layerIndex]);        
    }
    
    private void drawMapLayer(Canvas canvas, final CoordRect area, final MapLayer layer) {
    	int my = area.topLeft.y;
    	int py = (area.topLeft.y - mapViewArea.topLeft.y) * displayTileSize;
    	int px0 = (area.topLeft.x - mapViewArea.topLeft.x) * displayTileSize;
		for (int y = 0; y < area.size.height; ++y, ++my, py += displayTileSize) {
        	int mx = area.topLeft.x;
        	int px = px0;
        	for (int x = 0; x < area.size.width; ++x, ++mx, px += displayTileSize) {
        		final int tile = layer.tiles[mx][my];
        		if (tile != 0) {
        			canvas.drawBitmap(tiles.bitmaps[tile], px, py, mPaint);
        		}
            }
        }
    }

	private void drawFromMapPosition(Canvas canvas, final CoordRect area, final Coord p, final int tile) {
		drawFromMapPosition(canvas, area, p.x, p.y, tile);
    }

	private void drawFromMapPosition(Canvas canvas, final CoordRect area, int x, int y, final int tile) {
		if (!area.contains(x, y)) return;
		
    	x -= mapViewArea.topLeft.x;
    	y -= mapViewArea.topLeft.y;
		if (	   (x >= 0 && x < mapViewArea.size.width)
				&& (y >= 0 && y < mapViewArea.size.height)) {
			canvas.drawBitmap(tiles.bitmaps[tile], 
	        		x * displayTileSize,
	        		y * displayTileSize,
	        		mPaint);
		}
    }
	
	private void drawEffectText(Canvas canvas, final CoordRect area, final EffectAnimation e) {
    	int x = (e.position.x - mapViewArea.topLeft.x) * displayTileSize + displayTileSize/2;
    	int y = (e.position.y - mapViewArea.topLeft.y) * displayTileSize + displayTileSize/2 + e.textYOffset;
		canvas.drawText(e.displayText, x, y, e.textPaint);
    }
    
	public void notifyMapChanged() {
		Size mapViewSize = new Size(
    			Math.min(screenSizeTileCount.width, model.currentMap.size.width)
    			,Math.min(screenSizeTileCount.height, model.currentMap.size.height)
			);
		mapViewArea = new CoordRect(mapTopLeft, mapViewSize);
		notifyPlayerMoved();
		doubleBufferCanvas.drawColor(Color.BLACK);
		redrawAll();
	}
	public void notifyPlayerMoved() {
		mapTopLeft.set(0, 0);
		
		final LayeredWorldMap currentMap = model.currentMap;
		final Coord playerpos = model.player.position;
		
    	if (currentMap.size.width > screenSizeTileCount.width) {
    		mapTopLeft.x = Math.max(0, playerpos.x - mapViewArea.size.width/2);
    		mapTopLeft.x = Math.min(mapTopLeft.x, currentMap.size.width - mapViewArea.size.width);
    	}
    	if (currentMap.size.height > screenSizeTileCount.height) {
    		mapTopLeft.y = Math.max(0, playerpos.y - mapViewArea.size.height/2);
    		mapTopLeft.y = Math.min(mapTopLeft.y, currentMap.size.height - mapViewArea.size.height);
    	}
	}
}