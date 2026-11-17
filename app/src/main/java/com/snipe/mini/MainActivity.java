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
  int state=MENU, best, lives=3, coins=0, wallet=0, skin=0, unlocked=1, mode=0, level=1, totalRuns=0, totalCoins=0, kills=0, dailyDay=0, missionRewarded=0;\n  int menuPage=0, bossHp=0, bossMax=0, bossStage=0;\n  float magnet=0, slow=0, bossX=480, bossTime=0;\n  final int[] skins={0xff36f5df,0xffffd66b,0xffff4c83,0xffae8bff,0xff73ff85,0xffffffff};
  boolean muted=false, dragging=false, audioRunning=true;
  float px=480,tx=480,py=420,elapsed=0,spawn=0,score=0,shield=0,invuln=0,shake=0,background=0;
  long last=System.nanoTime();
  AudioTrack audio; Thread musicThread;
  final float[] starsX=new float[64],starsY=new float[64],starsS=new float[64];
  class Obstacle {float x,y,w,h,speed;int type;Obstacle(float xx,float yy,float ww,float hh,float sp,int t){x=xx;y=yy;w=ww;h=hh;speed=sp;type=t;}}
  class Particle {float x,y,vx,vy,life;int color;Particle(float xx,float yy,float vxx,float vyy,int c){x=xx;y=yy;vx=vxx;vy=vyy;life=.55f;color=c;}}
  Game(Context ctx){
   super(ctx);setLayerType(View.LAYER_TYPE_HARDWARE,null);
   prefs=ctx.getSharedPreferences("snipe_dodge_v2",0);best=prefs.getInt("best",0);wallet=prefs.getInt("wallet",0);skin=prefs.getInt("skin",0);unlocked=prefs.getInt("unlocked",1);level=prefs.getInt("level",1);totalRuns=prefs.getInt("runs",0);totalCoins=prefs.getInt("coins",0);dailyDay=prefs.getInt("daily",0);missionRewarded=prefs.getInt("mission",0);muted=prefs.getBoolean("muted",false);
   for(int i=0;i<starsX.length;i++){starsX[i]=rnd.nextInt(960);starsY[i]=rnd.nextInt(540);starsS[i]=.3f+rnd.nextFloat()*1.6f;}
   startAudio();setFocusable(true);
  }
  void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
  void round(Canvas c,float l,float t,float r,float b,float rad,int color){fill(color);rect.set(l,t,r,b);c.drawRoundRect(rect,rad,rad,p);}
  void label(Canvas c,String s,float x,float y,int size,int color){fill(color);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif",1));p.setTextSize(size);c.drawText(s,x,y,p);}
  void button(Canvas c,String s,float l,float t,float r,float b,int color){round(c,l,t,r,b,14,color);label(c,s,(l+r)/2,(t+b)/2+8,23,0xff071326);}
  void save(){prefs.edit().putInt("best",best).putInt("wallet",wallet).putInt("skin",skin).putInt("unlocked",unlocked).putInt("level",level).putInt("runs",totalRuns).putInt("coins",totalCoins).putInt("daily",dailyDay).putInt("mission",missionRewarded).apply();}\n  void begin(){obstacles.clear();particles.clear();px=480;tx=480;elapsed=0;spawn=.8f;score=0;shield=0;invuln=0;shake=0;lives=mode==2?1:3;coins=0;magnet=0;slow=0;bossHp=0;bossStage=0;totalRuns++;save();dragging=false;state=PLAY;last=System.nanoTime();}
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
       double low=Math.sin(2*Math.PI*bass*sec)*.055;
       double lead=Math.sin(2*Math.PI*bass*2*sec)*.018*(1-phase);
       double kick=Math.sin(2*Math.PI*(65-40*phase)*sec)*Math.exp(-phase*17)*.035;
       buf[i]=(short)(Math.max(-1,Math.min(1,low+lead+kick))*8500);
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
   int roll=rnd.nextInt(100);int type=roll<11?2:roll<30?1:roll<35?3:roll<40?4:0;
   float w=type==0?38+rnd.nextInt(62):type==1?30:34;
   float x=35+rnd.nextInt(880-(int)w);
   obstacles.add(new Obstacle(x,-50,w,type==0?36+rnd.nextInt(30):34,185+elapsed*4+rnd.nextInt(65),type));
  }
  void update(float dt){
   background+=dt;shake=Math.max(0,shake-dt*3);
   for(int i=particles.size()-1;i>=0;i--){Particle a=particles.get(i);a.x+=a.vx*dt;a.y+=a.vy*dt;a.life-=dt;if(a.life<=0)particles.remove(i);}
   if(state!=PLAY)return;
   elapsed+=dt;score+=dt*(mode==2?19:12);shield=Math.max(0,shield-dt);magnet=Math.max(0,magnet-dt);slow=Math.max(0,slow-dt);invuln=Math.max(0,invuln-dt);
   px+=(tx-px)*Math.min(1,dt*17);
   px=Math.max(28,Math.min(932,px));
   spawn-=dt;
   if(spawn<=0&&obstacles.size()<35){addObstacle();spawn=Math.max(.19f,(mode==2?.55f:mode==1?.64f:.8f)-elapsed*.008f);}
   for(int i=obstacles.size()-1;i>=0;i--){
    Obstacle b=obstacles.get(i);b.y+=b.speed*dt*(slow>0?.48f:1f);if(magnet>0&&b.type==1&&Math.abs(px-b.x)<180){b.x+=(px-b.x)*dt*3;}
    if(b.y>590){obstacles.remove(i);continue;}
    if(Math.abs((b.x+b.w/2)-px)<b.w/2+22&&Math.abs((b.y+b.h/2)-py)<b.h/2+21){
     obstacles.remove(i);
     if(b.type==1){coins++;wallet++;totalCoins++;score+=45;save();burst(px,py,GOLD,13);}
     else if(b.type==2){shield=5;burst(px,py,CYAN,14);}\n     else if(b.type==3){magnet=7;burst(px,py,GOLD,14);}\n     else if(b.type==4){slow=6;burst(px,py,0xffae8bff,14);}
     else if(shield>0){shield=0;burst(px,py,CYAN,18);}
     else if(invuln<=0){lives--;invuln=1.2f;shake=1;burst(px,py,PINK,25);if(lives<=0){state=OVER;int s=(int)score;if(s>best)best=s;level=Math.max(level,1+(int)(score/250));save();}}
    }
   }
   if(rnd.nextFloat()<dt*12)burst(px,py+23,skins[skin],1);\n   if(mode==1){int stage=1+(int)(elapsed/40);if(stage>bossStage){bossStage=stage;bossHp=5+stage*2;bossMax=bossHp;}if(bossHp>0){bossTime+=dt;bossX=480+(float)Math.sin(bossTime*1.4)*290;if(bossTime>1.6f){bossTime=0;obstacles.add(new Obstacle(bossX,-20,70,28,270+stage*12,0));}if(Math.abs(px-bossX)<75&&rnd.nextFloat()<dt*.5f){bossHp--;score+=20;burst(bossX,100,PINK,5);}}}\n   level=Math.max(level,1+(int)(score/250));
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
    int color=b.type==1?GOLD:b.type==2?CYAN:b.type==3?0xff77ff88:b.type==4?0xffae8bff:PINK;
    round(c,b.x,b.y,b.x+b.w,b.y+b.h,8,color);
    if(b.type!=0){label(c,b.type==1?"+":b.type==2?"S":b.type==3?"M":"T",b.x+b.w/2,b.y+b.h/2+8,22,0xff071326);}
   }
   for(Particle a:particles){fill(a.color);c.drawCircle(a.x,a.y,Math.max(1,a.life*5),p);}
   if(state!=MENU){
    if(shield>0){fill(0x6636f5df);c.drawCircle(px,py,39,p);}
    if(invuln<=0||((int)(invuln*12)%2==0)){
     round(c,px-23,py-23,px+23,py+23,11,skins[skin]);
     fill(0xff071326);c.drawCircle(px-8,py-5,4,p);c.drawCircle(px+8,py-5,4,p);
     round(c,px-9,py+7,px+9,py+10,2,0xff071326);
    }
   }
   c.restore();
   if(state==PLAY||state==PAUSE){
    label(c,"SCORE "+(int)score,130,48,24,WHITE);
    label(c,"BEST "+best,465,48,20,0xff9aaed1);
    label(c,"HP "+lives+"   COINS "+coins+"   LV "+level,745,48,19,GOLD);\n    if(mode==1&&bossHp>0){round(c,345,68,615,78,4,0xff35415e);round(c,345,68,345+270f*bossHp/bossMax,78,4,PINK);}
    round(c,875,16,947,66,12,0xff293554);label(c,"II",911,51,25,WHITE);
   }
   if(state==MENU){
    label(c,"SNIPE DODGE",480,112,58,CYAN);
    label(c,"NEON SURVIVAL  •  VERSION 3.0",480,148,18,0xff9baad4);
    button(c,"PLAY",345,169,615,219,CYAN);
    button(c,"MODES",205,238,470,286,0xffd8e2ff);
    button(c,"SHOP",490,238,755,286,GOLD);
    button(c,"MISSIONS",205,302,470,350,0xffd8e2ff);
    button(c,"STATS",490,302,755,350,0xffd8e2ff);
    button(c,"DAILY REWARD",345,368,615,416,GOLD);
    label(c,"BEST "+best+"   COINS "+wallet+"   LEVEL "+level,480,458,21,WHITE);
    label(c,muted?"SOUND OFF":"SOUND ON",830,514,20,CYAN);
    }else if(state==MENU+4){
    label(c,menuPage==1?"GAME MODES":menuPage==2?"SKIN SHOP":menuPage==3?"MISSIONS":"STATISTICS",480,100,43,WHITE);
    if(menuPage==1){label(c,"SELECT MODE",480,160,21,CYAN);button(c,"CLASSIC",310,185,650,235,mode==0?CYAN:0xffd8e2ff);button(c,"BOSS RUSH",310,250,650,300,mode==1?CYAN:0xffd8e2ff);button(c,"HARDCORE",310,315,650,365,mode==2?CYAN:0xffd8e2ff);}
    if(menuPage==2){label(c,"WALLET: "+wallet+"   •   SKIN: "+(skin+1)+"/6",480,163,24,GOLD);for(int i=0;i<6;i++){float x=215+i*106;round(c,x,205,x+72,277,14,skins[i]);label(c,(unlocked&(1<<i))!=0?"OWNED":"50",x+36,309,17,WHITE);}label(c,"TAP A SKIN TO EQUIP OR BUY",480,355,18,CYAN);}
    if(menuPage==3){label(c,"COLLECT 20 COINS  •  "+totalCoins+"/20",480,180,23,GOLD);label(c,"PLAY 5 RUNS  •  "+totalRuns+"/5",480,228,23,CYAN);label(c,"REACH LEVEL 5  •  "+level+"/5",480,276,23,WHITE);button(c,"CLAIM MISSION REWARD",300,325,660,385,GOLD);label(c,"ONE-TIME REWARD: 100 COINS",480,416,18,WHITE);}
    if(menuPage==4){label(c,"BEST SCORE: "+best,480,165,28,GOLD);label(c,"TOTAL RUNS: "+totalRuns,480,220,25,WHITE);label(c,"COINS COLLECTED: "+totalCoins,480,275,25,CYAN);label(c,"LEVEL: "+level,480,330,25,WHITE);}
    button(c,"BACK",375,465,585,515,CYAN);
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
    if(state==MENU){if(y>165&&y<224&&x>345&&x<615){begin();}else if(y>235&&y<290){menuPage=x<480?1:2;state=MENU+4;}else if(y>299&&y<355){menuPage=x<480?3:4;state=MENU+4;}else if(y>365&&y<423&&x>345&&x<615){int today=(int)(System.currentTimeMillis()/86400000L);if(today>dailyDay){dailyDay=today;wallet+=25;save();burst(480,380,GOLD,30);}}else if(y>485&&x>745){muted=!muted;prefs.edit().putBoolean("muted",muted).apply();if(audio!=null){try{if(muted)audio.pause();else audio.play();}catch(Exception ignored){}}}}
    else if(state==MENU+4){if(y>460){state=MENU;}else if(menuPage==1){if(y>180&&y<240)mode=0;else if(y>245&&y<305)mode=1;else if(y>310&&y<370)mode=2;}else if(menuPage==2&&y>200&&y<320){int idx=(int)((x-215)/106);if(idx>=0&&idx<6&&x>=215+idx*106&&x<=287+idx*106){if((unlocked&(1<<idx))!=0)skin=idx;else if(wallet>=50){wallet-=50;unlocked|=1<<idx;skin=idx;}save();}}else if(menuPage==3&&y>320&&y<390&&missionRewarded==0&&totalCoins>=20&&totalRuns>=5&&level>=5){wallet+=100;missionRewarded=1;save();}}\n    else if(state==PLAY){if(x>860&&y<80){state=PAUSE;dragging=false;}else{dragging=true;tx=x;}}
    else if(state==PAUSE||state==OVER){if(y>275&&y<340){if(state==OVER)begin();else{state=PLAY;last=System.nanoTime();}}else if(y>345&&y<410){state=MENU;}}
   }else if(action==MotionEvent.ACTION_MOVE&&state==PLAY&&dragging)tx=x;
   else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)dragging=false;
   invalidate();return true;
  }
 }
}
