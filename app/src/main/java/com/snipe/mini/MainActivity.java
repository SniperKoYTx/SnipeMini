package com.snipe.mini;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
  setContentView(new Game());
 }
 class Game extends View {
  final Paint p = new Paint(3);
  final Random rng = new Random();
  final ArrayList<RectF> blocks = new ArrayList<>();
  float playerX=480, playerY=420, elapsed=0, spawn=0, score=0;
  long last=System.nanoTime();
  boolean dead=false;
  Game(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);}
  void text(Canvas c,String s,float x,float y,int size,int clr){
   color(clr);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.create("sans-serif-black",1));p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);
  }
  void reset(){blocks.clear();playerX=480;elapsed=0;spawn=0;score=0;dead=false;last=System.nanoTime();}
  @Override protected void onDraw(Canvas original){
   super.onDraw(original);
   float sx=getWidth()/960f,sy=getHeight()/540f;
   Canvas c=original;c.save();c.scale(sx,sy);
   long now=System.nanoTime();float dt=Math.min(.05f,(now-last)/1000000000f);last=now;
   c.drawColor(Color.rgb(10,13,30));
   color(Color.rgb(20,25,51));
   for(int i=0;i<13;i++)c.drawLine(i*80,0,i*80,540,p);
   for(int i=0;i<8;i++)c.drawLine(0,i*80,960,i*80,p);
   if(!dead){
    elapsed+=dt;score+=dt*10;spawn-=dt;
    if(spawn<=0){float w=35+rng.nextInt(55);float x=rng.nextInt(920);blocks.add(new RectF(x,-80,x+w,-20));spawn=Math.max(.19f,.65f-elapsed*.008f);}
    Iterator<RectF> it=blocks.iterator();
    while(it.hasNext()){
     RectF b=it.next();b.offset(0,(210+elapsed*5)*dt);
     if(b.top>550){it.remove();continue;}
     if(RectF.intersects(b,new RectF(playerX-25,playerY-24,playerX+25,playerY+24)))dead=true;
    }
   }
   color(Color.rgb(255,70,112));
   for(RectF b:blocks){c.drawRoundRect(b,8,8,p);}
   color(Color.rgb(0,226,204));c.drawRoundRect(new RectF(playerX-26,playerY-26,playerX+26,playerY+26),9,9,p);
   color(Color.WHITE);c.drawCircle(playerX-8,playerY-6,4,p);c.drawCircle(playerX+8,playerY-6,4,p);
   text(c,"SNIPE DODGE",480,55,33,Color.WHITE);
   text(c,"SCORE: "+(int)score,480,95,23,Color.rgb(0,226,204));
   text(c,"DRAG TO MOVE  •  DODGE RED BLOCKS",480,518,19,Color.rgb(180,188,215));
   if(dead){
    color(0xDD080C1C);c.drawRoundRect(new RectF(260,155,700,365),25,25,p);
    text(c,"GAME OVER",480,233,45,Color.WHITE);
    text(c,"SCORE: "+(int)score,480,279,26,0xFF00E2CC);
    text(c,"TAP TO RESTART",480,326,22,0xFFFF7799);
   }
   c.restore();postInvalidateDelayed(16);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   if(e.getAction()==MotionEvent.ACTION_DOWN && dead){reset();return true;}
   if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE){
    playerX=Math.max(28,Math.min(932,e.getX()*960/getWidth()));
    return true;
   }
   return true;
  }
 }
}
