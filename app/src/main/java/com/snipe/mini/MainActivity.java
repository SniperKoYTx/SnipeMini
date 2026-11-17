package com.snipe.mini;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {
 Game game;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  game=new Game(this);setContentView(game);
 }
 @Override protected void onPause(){super.onPause();if(game!=null)game.suspend();}
 @Override protected void onResume(){super.onResume();if(game!=null)game.resumeAudio();}
 @Override protected void onDestroy(){if(game!=null)game.closeAudio();super.onDestroy();}
 @Override public void onBackPressed(){if(game!=null)game.back();else super.onBackPressed();}

 class Game extends View {
  static final int MENU=0,PLAY=1,PAUSE=2,OVER=3;
  final Paint p=new Paint(3);
  final Random rnd=new Random();
  final ArrayList<Obstacle> obstacles=new ArrayList<>();
  final ArrayList<Particle> particles=new ArrayList<>();
  final SharedPreferences prefs;
  final RectF rect=new RectF();
  final int WHITE=Color.WHITE, CYAN=0xff36f5df, PINK=0xffff4c83, GOLD=0xffffd66b;
  int state=MENU, best, lives=3, coins=0;
  boolean muted=false, dragging=false, audioRunning=true;
  float px=480,tx=480,py=420,elapsed=0,spawn=0,score=0,shield=0,invuln=0,shake=0,background=0;
  long last=System.nanoTime();
  AudioTrack audio; Thread musicThread;
  final float[] starsX=new float[64],starsY=new float[64],starsS=new float[64];
  class Obstacle {float x,y,w,h,speed;int type;Obstacle(float xx,float yy,float ww,float hh,float sp,int t){x=xx;y=yy;w=ww;h=hh;speed=sp;type=t;}}
  class Particle {float x,y,vx,vy,life;int color;Particle(float xx,float yy,float vxx,float vyy,int c){x=xx;y=yy;vx=vxx;vy=vyy;life=.55f;color=c;}}
  Game(Context ctx){
   super(ctx);setLayerType(View.LAYER_TYPE_HARDWARE,null);
   prefs=ctx.getSharedPreferences("snipe_dodge_v2",0);best=prefs.getInt("best",0);muted=prefs.getBoolean("muted",false);
   for(int i=0;i<starsX.length;i++){starsX[i]=rnd.nextInt(960);starsY[i]=rnd.nextInt(540);starsS[i]=.3f+rnd.nextFloat()*1.6f;}
   startAudio();setFocusable(true);
  }
  void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
  void round(Canvas c,float l,float t,float r,float b,float rad,int color){fill(color);rect.set(l,t,r,b);c.drawRoundRect(rect,rad,rad,p);}
  void label(Canvas c,String s,float x,float y,int size,int color){fill(color);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif",1));p.setTextSize(size);c.drawText(s,x,y,p);}
  void button(Canvas c,String s,float l,float t,float r,float b,int color){round(c,l,t,r,b,14,color);label(c,s,(l+r)/2,(t+b)/2+8,23,0xff071326);}
  void begin(){obstacles.clear();particles.clear();px=480;tx=480;elapsed=0;spawn=.8f;score=0;shield=0;invuln=0;shake=0;lives=3;coins=0;dragging=false;state=PLAY;last=System.nanoTime();}
  void back(){if(state==PLAY)state=PAUSE;else if(state==PAUSE)state=PLAY;else state=MENU;invalidate();}
  void suspend(){if(state==PLAY)state=PAUSE;audioRunning=false;if(audio!=null){try{audio.pause();}catch(Exception ignored){}}}
  void resumeAudio(){audioRunning=true;last=System.nanoTime();if(audio!=null&&!muted){try{audio.play();}catch(Exception ignored){}}}
  void closeAudio(){audioRunning=false;if(musicThread!=null)musicThread.interrupt();if(audio!=null){try{audio.pause();audio.flush();audio.release();}catch(Exception ignored){}audio=null;}}
  void startAudio(){
   try {
    final int rate=22050;
    int size=Math.max(4096,AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT));
    audio=new AudioTrack(AudioManager.STREAM_MUSIC,rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,size,AudioTrack.MODE_STREAM);
    if(!muted)audio.play();
    musicThread=new Thread(new Runnable(){public void run(){
     short[] buf=new short[2048];long sample=0;
     final double[] notes={110,146.83,164.81,130.81,110,146.83,196,164.81};
     while(!Thread.currentThread().isInterrupted()){
      if(!audioRunning||muted){try{Thread.sleep(90);}catch(InterruptedException e){break;}continue;}
      for(int i=0;i<buf.length;i++,sample++){
       double sec=(double)sample/rate, beat=sec*2.4, phase=beat%1;
       int step=((int)beat/2)%notes.length;
       double bass=notes[step];
       double low=Math.sin(2*Math.PI*bass*sec)*.16;
       double lead=Math.sin(2*Math.PI*bass*2*sec)*.055*(1-phase);
       double kick=Math.sin(2*Math.PI*(65-40*phase)*sec)*Math.exp(-phase*17)*.14;
       buf[i]=(short)(Math.max(-1,Math.min(1,low+lead+kick))*14000);
      }
      try{audio.write(buf,0,buf.length);}catch(Exception e){break;}
     }
    }},"SnipeMusic");musicThread.setDaemon(true);musicThread.start();
   }catch(Exception ignored){muted=true;}
  }
  void burst(float x,float y,int color,int count){
   for(int i=0;i<count&&particles.size()<110;i++){
    float a=rnd.nextFloat()*6.283f,s=45+rnd.nextFloat()*180;
    particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,color));
   }
  }
  void addObstacle(){
   int type=rnd.nextInt(100)<12?2:(rnd.nextInt(100)<18?1:0);
   float w=type==0?38+rnd.nextInt(62):type==1?30:34;
   float x=35+rnd.nextInt(880-(int)w);
   obstacles.add(new Obstacle(x,-50,w,type==0?36+rnd.nextInt(30):34,185+elapsed*4+rnd.nextInt(65),type));
  }
  void update(float dt){
   background+=dt;shake=Math.max(0,shake-dt*3);
   for(int i=particles.size()-1;i>=0;i--){Particle a=particles.get(i);a.x+=a.vx*dt;a.y+=a.vy*dt;a.life-=dt;if(a.life<=0)particles.remove(i);}
   if(state!=PLAY)return;
   elapsed+=dt;score+=dt*12;shield=Math.max(0,shield-dt);invuln=Math.max(0,invuln-dt);
   px+=(tx-px)*Math.min(1,dt*17);
   px=Math.max(28,Math.min(932,px));
   spawn-=dt;
   if(spawn<=0&&obstacles.size()<35){addObstacle();spawn=Math.max(.22f,.78f-elapsed*.009f);}
   for(int i=obstacles.size()-1;i>=0;i--){
    Obstacle b=obstacles.get(i);b.y+=b.speed*dt;
    if(b.y>590){obstacles.remove(i);continue;}
    if(Math.abs((b.x+b.w/2)-px)<b.w/2+22&&Math.abs((b.y+b.h/2)-py)<b.h/2+21){
     obstacles.remove(i);
     if(b.type==1){coins++;score+=45;burst(px,py,GOLD,13);}
     else if(b.type==2){shield=5;burst(px,py,CYAN,14);}
     else if(shield>0){shield=0;burst(px,py,CYAN,18);}
     else if(invuln<=0){lives--;invuln=1.2f;shake=1;burst(px,py,PINK,25);if(lives<=0){state=OVER;int s=(int)score;if(s>best){best=s;prefs.edit().putInt("best",best).apply();}}}
    }
   }
   if(rnd.nextFloat()<dt*22)burst(px,py+23,CYAN,1);
  }
  @Override protected void onDraw(Canvas raw){
   super.onDraw(raw);
   float sx=getWidth()/960f,sy=getHeight()/540f;
   long now=System.nanoTime();float dt=Math.min(.035f,Math.max(0,(now-last)/1000000000f));last=now;
   update(dt);
   Canvas c=raw;c.save();c.scale(sx,sy);
   c.drawColor(0xff080d21);
   fill(0xff1a2346);p.setStrokeWidth(1);
   for(int i=0;i<13;i++)c.drawLine(i*80,0,i*80,540,p);
   for(int i=0;i<8;i++)c.drawLine(0,i*80,960,i*80,p);
   for(int i=0;i<starsX.length;i++){fill(0xff35476b);c.drawCircle(starsX[i],(starsY[i]+background*starsS[i]*12)%540,starsS[i],p);}
   c.save();if(shake>0)c.translate((rnd.nextFloat()-.5f)*shake*10,(rnd.nextFloat()-.5f)*shake*10);
   for(Obstacle b:obstacles){
    int color=b.type==1?GOLD:b.type==2?CYAN:PINK;
    round(c,b.x,b.y,b.x+b.w,b.y+b.h,8,color);
    if(b.type!=0){label(c,b.type==1?"+":"S",b.x+b.w/2,b.y+b.h/2+8,22,0xff071326);}
   }
   for(Particle a:particles){fill(a.color);c.drawCircle(a.x,a.y,Math.max(1,a.life*5),p);}
   if(state!=MENU){
    if(shield>0){fill(0x6636f5df);c.drawCircle(px,py,39,p);}
    if(invuln<=0||((int)(invuln*12)%2==0)){
     round(c,px-23,py-23,px+23,py+23,11,CYAN);
     fill(0xff071326);c.drawCircle(px-8,py-5,4,p);c.drawCircle(px+8,py-5,4,p);
     round(c,px-9,py+7,px+9,py+10,2,0xff071326);
    }
   }
   c.restore();
   if(state==PLAY||state==PAUSE){
    label(c,"SCORE "+(int)score,130,48,24,WHITE);
    label(c,"BEST "+best,465,48,20,0xff9aaed1);
    label(c,"HP "+lives+"   + "+coins,755,48,22,GOLD);
    round(c,875,16,947,66,12,0xff293554);label(c,"II",911,51,25,WHITE);
   }
   if(state==MENU){
    label(c,"SNIPE",480,145,76,CYAN);
    label(c,"DODGE",480,207,67,WHITE);
    label(c,"NEON SURVIVAL",480,246,19,0xff9baad4);
    button(c,"PLAY",350,292,610,354,CYAN);
    button(c,muted?"MUSIC OFF":"MUSIC ON",350,370,610,420,0xffd8e2ff);
    label(c,"BEST SCORE  "+best,480,466,22,GOLD);
    label(c,"MOVE WITH YOUR FINGER  •  COLLECT GOLD & SHIELDS",480,511,17,0xff9baad4);
   }else if(state==PAUSE||state==OVER){
    round(c,265,130,695,425,24,0xee121a35);
    label(c,state==OVER?"GAME OVER":"PAUSED",480,203,46,WHITE);
    label(c,state==OVER?"SCORE  "+(int)score+"  •  BEST  "+best:"TAKE A BREAK",480,253,22,GOLD);
    button(c,state==OVER?"RETRY":"RESUME",332,278,628,333,CYAN);
    button(c,"MAIN MENU",332,350,628,401,0xffd8e2ff);
   }else if(state==PLAY){label(c,"DRAG LEFT OR RIGHT TO DODGE",480,519,17,0xff9baad4);}
   c.restore();postInvalidateDelayed(16);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   int action=e.getActionMasked();float x=e.getX()*960/getWidth(),y=e.getY()*540/getHeight();
   if(action==MotionEvent.ACTION_DOWN){
    if(state==MENU){if(y>292&&y<356){begin();}else if(y>365&&y<430){muted=!muted;prefs.edit().putBoolean("muted",muted).apply();if(audio!=null){try{if(muted)audio.pause();else audio.play();}catch(Exception ignored){}}}}
    else if(state==PLAY){if(x>860&&y<80){state=PAUSE;dragging=false;}else{dragging=true;tx=x;}}
    else if(state==PAUSE||state==OVER){if(y>275&&y<340){if(state==OVER)begin();else{state=PLAY;last=System.nanoTime();}}else if(y>345&&y<410){state=MENU;}}
   }else if(action==MotionEvent.ACTION_MOVE&&state==PLAY&&dragging)tx=x;
   else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)dragging=false;
   invalidate();return true;
  }
 }
}
