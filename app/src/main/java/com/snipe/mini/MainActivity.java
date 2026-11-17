package com.snipe.mini;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.media.*;
import java.util.*;

public class MainActivity extends Activity {
 Engine game;
 public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);game=new Engine(this);setContentView(game);}
 protected void onPause(){super.onPause();game.pause();}
 protected void onResume(){super.onResume();if(game!=null)game.resume();}
 protected void onDestroy(){if(game!=null)game.shutdown();super.onDestroy();}\n public void onBackPressed(){game.back();}
 static class Engine extends View {
  static final int HOME=0,PLAY=1,PAUSE=2,DEAD=3,MODES=4,SHOP=5,QUEST=6,STATS=7,OPTIONS=8,TUTORIAL=9,CHEST=10;
  final Paint p=new Paint(3); final RectF rect=new RectF(); final Random rng=new Random(812); final SharedPreferences pref;
  final ArrayList<Obj> objects=new ArrayList<>(); final ArrayList<Particle> particles=new ArrayList<>();
  final int[] palette={0xff28ead7,0xffffcc54,0xffff578e,0xff9b79ff,0xff77f48e,0xfff5f5fa,0xffed733d,0xff4b9eff,0xfff2b3ee,0xffb6f65e,0xff777777,0xffe9bc65};
  final String[] skinNames={"SNIPE BOT","SOLAR","PULSE","GHOST","LIME","SNOW","EMBER","OCEAN","BUBBLE","TOXIC","STEEL","LEGEND"};
  final String[] worlds={"NEON CITY","DEEP SPACE","LAVA CORE","FROZEN GRID"};
  final int[] backgrounds={0xff080f28,0xff090b1d,0xff26100d,0xff092b3e};
  final int[] accents={0xff28ead7,0xffac85ff,0xffff8051,0xff89eaff};
  int state=HOME,mode=0,world=0,skin=0,unlocked=1,coins=0,best=0,runs=0,totalCoins=0,level=1,achievements=0;
  int dailyStamp=0,weekStamp=0,dayCoins=0,weekRuns=0,dayRuns=0,claims=0,quality=1,fps=60,control=0, music=0,effects=1;
  int lives=3,combo=0,maxCombo=0,near=0,bossHp=0,bossMax=0,bossNumber=0,shots=0;
  float x=480,target=480,y=425,score=0,elapsed=0,spawn=.7f,invincible=0,shield=0,magnet=0,slow=0,dash=0,dashCooldown=0,shake=0,clock=0,bossTimer=0;
  boolean touched=false,seenTutorial=false;
  long last=System.nanoTime(),nextFrame=0; ToneGenerator tone; int soundCoin=1,soundHit=2,soundPower=3;
  static class Obj{float x,y,w,h,speed;int kind;boolean grazed;Obj(float x,float y,float w,float h,float speed,int kind){this.x=x;this.y=y;this.w=w;this.h=h;this.speed=speed;this.kind=kind;}}
  static class Particle{float x,y,vx,vy,t;int c;Particle(float x,float y,float vx,float vy,int c){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.c=c;this.t=.5f;}}
  Engine(Context c){super(c);pref=c.getSharedPreferences("snipe_dodge_v2",0);load();setLayerType(View.LAYER_TYPE_HARDWARE,null);initSounds();}
  int get(String k,int d){return pref.getInt(k,d);}
  void load(){coins=get("wallet",0);best=get("best",0);runs=get("runs",0);totalCoins=get("coins",0);level=get("level",1);skin=Math.min(11,get("skin",0));unlocked=get("unlocked",1);mode=get("mode",0);world=get("world",0);achievements=get("achievements",0);dailyStamp=get("daily",0);weekStamp=get("week",0);dayCoins=get("dayCoins",0);weekRuns=get("weekRuns",0);dayRuns=get("dayRuns",0);claims=get("claims",0);fps=get("fps",60);quality=get("quality",1);control=get("control",0);music=get("music",0);effects=get("effects",1);seenTutorial=get("tutorial",0)==1;rollover();}
  void save(){pref.edit().putInt("wallet",coins).putInt("best",best).putInt("runs",runs).putInt("coins",totalCoins).putInt("level",level).putInt("skin",skin).putInt("unlocked",unlocked).putInt("mode",mode).putInt("world",world).putInt("achievements",achievements).putInt("daily",dailyStamp).putInt("week",weekStamp).putInt("dayCoins",dayCoins).putInt("weekRuns",weekRuns).putInt("dayRuns",dayRuns).putInt("claims",claims).putInt("fps",fps).putInt("quality",quality).putInt("control",control).putInt("music",music).putInt("effects",effects).putInt("tutorial",seenTutorial?1:0).apply();}
  void rollover(){Calendar c=Calendar.getInstance();int day=c.get(Calendar.YEAR)*400+c.get(Calendar.DAY_OF_YEAR);int week=c.get(Calendar.YEAR)*60+c.get(Calendar.WEEK_OF_YEAR);if(day!=dailyStamp){dailyStamp=day;dayCoins=0;dayRuns=0;claims&=~3;}if(week!=weekStamp){weekStamp=week;weekRuns=0;claims&=~4;}save();}
  void initSounds(){try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,35);}catch(Exception e){tone=null;}}
  void sfx(int id){if(effects==1&&tone!=null)tone.startTone(id==1?ToneGenerator.TONE_PROP_ACK:id==2?ToneGenerator.TONE_PROP_NACK:ToneGenerator.TONE_PROP_BEEP,65);}
  void shutdown(){if(tone!=null){tone.release();tone=null;}}
  void pause(){if(state==PLAY)state=PAUSE;last=System.nanoTime();}
  void resume(){last=System.nanoTime();}
  void back(){if(state==PLAY)state=PAUSE;else if(state==PAUSE)state=PLAY;else state=HOME;last=System.nanoTime();invalidate();}
  void start(){rollover();objects.clear();particles.clear();score=elapsed=0;spawn=.8f;invincible=shield=magnet=slow=dash=dashCooldown=0;bossHp=0;bossNumber=0;bossTimer=0;shots=0;combo=near=0;lives=mode==2?1:3;x=target=480;runs++;dayRuns++;weekRuns++;save();state=seenTutorial?PLAY:TUTORIAL;last=System.nanoTime();}
  void end(){state=DEAD;best=Math.max(best,(int)score);level=Math.max(level,1+(int)(Math.sqrt(best/100.0)));updateAchievements();save();}
  void updateAchievements(){int[] milestones={1,5,10,25,50,100,200,500,1000,2000,5,10,20,50,100,1,3,5,10,20};for(int i=0;i<20;i++){int v=i<10?totalCoins:i<15?runs:i<18?level:maxCombo;if(v>=milestones[i])achievements|=1<<i;}}
  void burst(float xx,float yy,int color,int count){if(quality==0)return;for(int i=0;i<count&&particles.size()<75;i++){double a=rng.nextDouble()*6.283;float speed=45+rng.nextFloat()*155;particles.add(new Particle(xx,yy,(float)Math.cos(a)*speed,(float)Math.sin(a)*speed,color));}}
  void spawn(){int roll=rng.nextInt(100);int type=roll<16?1:roll<23?2:roll<29?3:roll<35?4:0;float w=type==0?38+rng.nextInt(55):32;float xx=28+rng.nextInt((int)(900-w));objects.add(new Obj(xx,-65,w,type==0?38:32,175+Math.min(240,elapsed*3.3f)+rng.nextInt(55),type));}
  void boss(){if(mode!=1)return;int stage=1+(int)(elapsed/35);if(stage>bossNumber){bossNumber=stage;bossHp=7+stage*3;bossMax=bossHp;bossTimer=0;}if(bossHp<=0)return;bossTimer+=1f/60;float bx=480+(float)Math.sin(elapsed*1.7)*250;if(bossTimer>.8f){bossTimer=0;int attack=bossNumber%3;for(int i=0;i<(attack==2?3:1);i++){float ox=attack==0?x:bx+(i-1)*95;objects.add(new Obj(Math.max(30,Math.min(910,ox)),85,attack==1?85:32,25,240+bossNumber*15,0));}}}
  void tick(float dt){clock+=dt;for(int i=particles.size()-1;i>=0;i--){Particle a=particles.get(i);a.x+=a.vx*dt;a.y+=a.vy*dt;a.t-=dt;if(a.t<=0)particles.remove(i);}if(state!=PLAY)return;
   elapsed+=dt;score+=dt*(mode==2?18:12)*(1+Math.min(combo,20)*.02f);shield=Math.max(0,shield-dt);magnet=Math.max(0,magnet-dt);slow=Math.max(0,slow-dt);invincible=Math.max(0,invincible-dt);dash=Math.max(0,dash-dt);dashCooldown=Math.max(0,dashCooldown-dt);shake=Math.max(0,shake-dt*3);
   x+=(target-x)*Math.min(1,dt*(dash>0?35:16));x=Math.max(25,Math.min(935,x));
   spawn-=dt;if(spawn<=0&&objects.size()<38){spawn();spawn=Math.max(.25f,(mode==2?.54f:.82f)-elapsed*.006f);}
   if(mode==1){bossTimer+=dt;int stage=1+(int)(elapsed/40);if(stage>bossNumber){bossNumber=stage;bossHp=8+stage*2;bossMax=bossHp;}if(bossTimer>1.3f){bossTimer=0;float bx=480+(float)Math.sin(elapsed*1.8f)*280;int pattern=bossNumber%3;for(int j=0;j<(pattern==2?3:1);j++)objects.add(new Obj(Math.max(25,Math.min(920,pattern==0?x:bx+j*85-85)),-40,pattern==1?75:35,32,265+stage*10,0));}}
   for(int i=objects.size()-1;i>=0;i--){Obj o=objects.get(i);o.y+=o.speed*dt*(slow>0?.5f:1f);if(o.kind==1&&magnet>0&&Math.abs(x-o.x)<190)o.x+=(x-o.x)*dt*4;
    float dx=Math.abs(x-(o.x+o.w/2)),dy=Math.abs(y-(o.y+o.h/2));boolean hit=dx<o.w/2+18&&dy<o.h/2+18;
    if(o.kind==0&&!o.grazed&&dy<o.h/2+42&&dx<o.w/2+35&&!hit){o.grazed=true;near++;combo++;maxCombo=Math.max(maxCombo,combo);score+=5;}
    if(hit){objects.remove(i);if(o.kind==1){coins++;totalCoins++;dayCoins++;score+=25;sfx(soundCoin);burst(x,y,0xffffd66b,10);}
     else if(o.kind==2){shield=7;sfx(soundPower);burst(x,y,0xff28ead7,12);}
     else if(o.kind==3){magnet=7;sfx(soundPower);}
     else if(o.kind==4){slow=6;sfx(soundPower);}
     else if(shield>0||dash>0){shield=0;burst(x,y,0xff28ead7,12);}
     else if(invincible==0){lives--;combo=0;invincible=1.3f;shake=.8f;sfx(soundHit);burst(x,y,0xffff578e,20);if(lives<=0){end();break;}}}
    else if(o.y>570)objects.remove(i);
   }
   if(mode==1&&bossHp>0&&elapsed>10&&rng.nextFloat()<dt*.3f){bossHp--;score+=20;if(bossHp==0){coins+=30;score+=200;burst(480,100,0xffffd66b,35);}}
   if(quality>0&&rng.nextFloat()<dt*8)burst(x,y+24,palette[skin],1);
   if(((int)elapsed)%15==0&&rng.nextFloat()<dt*.4f)save();
  }
  void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);}
  void box(Canvas c,float l,float t,float r,float b,int col){color(col);rect.set(l,t,r,b);c.drawRoundRect(rect,12,12,p);}
  void txt(Canvas c,String s,float x,float y,int size,int col){color(col);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(s,x,y,p);}
  void btn(Canvas c,String s,float x,float y,float w,int col){box(c,x,y,x+w,y+49,col);txt(c,s,x+w/2,y+32,20,0xff071322);}
  void bot(Canvas c,float xx,float yy,int col,int size){float z=size/46f;c.save();c.translate(xx,yy);c.scale(z,z);box(c,-23,-22,23,22,col);box(c,-16,-11,-5,-2,0xff071322);box(c,5,-11,16,-2,0xff071322);box(c,-9,8,9,11,0xff071322);color(col);c.drawCircle(-19,24,6,p);c.drawCircle(19,24,6,p);c.restore();}
  void scenery(Canvas c){int bg=backgrounds[world];c.drawColor(bg);int accent=accents[world];color((accent&0x00ffffff)|0x33000000);for(int layer=0;layer<3;layer++){float speed=quality==0?0:clock*(layer+1)*7;for(int i=0;i<16;i++){float xx=(i*87+layer*33)%1020-30;float yy=(i*107+layer*53+(int)speed)%600-30;c.drawCircle(xx,yy,layer+1.5f,p);}}if(quality>0){color((accent&0x00ffffff)|0x19000000);for(int i=0;i<13;i++){float xx=i*80; c.drawLine(xx,0,xx,540,p);}for(int i=0;i<8;i++)c.drawLine(0,i*80,960,i*80,p);}}
  void header(Canvas c,String title){txt(c,title,480,85,43,0xffffffff);btn(c,"BACK",36,455,165,0xffd8e3f4);}
  protected void onDraw(Canvas raw){super.onDraw(raw);long now=System.nanoTime();float dt=Math.min(.04f,Math.max(0,(now-last)/1e9f));last=now;tick(dt);Canvas c=raw;c.save();c.scale(getWidth()/960f,getHeight()/540f);scenery(c);
   if(state==PLAY||state==PAUSE||state==DEAD){for(Obj o:objects){int col=o.kind==0?0xffff578e:o.kind==1?0xffffd66b:o.kind==2?0xff28ead7:o.kind==3?0xff83f48e:0xffa88bff;box(c,o.x,o.y,o.x+o.w,o.y+o.h,col);if(o.kind>0)txt(c,o.kind==1?"+":o.kind==2?"S":o.kind==3?"M":"T",o.x+o.w/2,o.y+24,20,0xff081222);}for(Particle a:particles){color(a.c);c.drawCircle(a.x,a.y,Math.max(1,a.t*6),p);}if(shield>0){color(0x5536f5df);c.drawCircle(x,y,39,p);}if(invincible==0||((int)(invincible*10))%2==0)bot(c,x,y,palette[skin],46);
    txt(c,"SCORE "+(int)score,120,43,22,0xffffffff);txt(c,"COMBO x"+combo,460,43,20,0xffffd66b);txt(c,"HP "+lives+"  COINS "+coins,743,43,19,0xffffffff);btn(c,"II",872,13,75,0xffd8e3f4);
    if(mode==1&&bossHp>0){box(c,340,70,620,82,0xff353653);box(c,340,70,340+280f*bossHp/bossMax,82,0xffff578e);txt(c,"BOSS "+(bossNumber%3+1),480,107,16,0xffffffff);}
    if(state==PLAY){txt(c,"DASH "+(dashCooldown==0?"READY":(int)Math.ceil(dashCooldown)+"s"),860,514,18,0xff28ead7);txt(c,worlds[world],165,514,16,0xffb5c4dd);}
   }
   if(state==HOME){bot(c,480,130,palette[skin],80);txt(c,"SNIPE DODGE 4.0",480,230,47,0xffffffff);txt(c,"SNIPE BOT  •  "+worlds[world],480,260,18,accents[world]);btn(c,"PLAY",346,282,268,0xff28ead7);btn(c,"MODES",160,348,202,0xffd8e3f4);btn(c,"SKINS",379,348,202,0xffffd66b);btn(c,"MISSIONS",598,348,202,0xffd8e3f4);btn(c,"STATS",160,411,202,0xffd8e3f4);btn(c,"CHEST",379,411,202,0xffffd66b);btn(c,"SETTINGS",598,411,202,0xffd8e3f4);txt(c,"COINS "+coins+"  •  BEST "+best+"  •  LEVEL "+level,480,510,19,0xffffffff);}
   if(state==MODES){header(c,"CHOOSE MODE / WORLD");String[] names={"CLASSIC","BOSS RUSH","HARDCORE"};for(int i=0;i<3;i++)btn(c,names[i],150+i*224,145,205,i==mode?0xff28ead7:0xffd8e3f4);for(int i=0;i<4;i++)btn(c,worlds[i]+(level<i*3+1?" LOCKED":""),55+i*224,270,210,i==world?accents[i]:0xffd8e3f4);txt(c,"UNLOCK WORLDS AT LEVEL 1, 4, 7, 10",480,391,19,0xffffffff);}
   if(state==SHOP){header(c,"SNIPE BOT SKINS");txt(c,"COINS "+coins+"  •  SELECT A BOT",480,132,22,0xffffd66b);for(int i=0;i<12;i++){int col=i%6,row=i/6;float xx=145+col*135,yy=205+row*130;bot(c,xx,yy,palette[i],45);txt(c,skinNames[i],xx,yy+45,13,0xffffffff);txt(c,(unlocked&(1<<i))!=0?(skin==i?"EQUIPPED":"OWNED"):(80+i*40)+" C",xx,yy+64,14,0xffffd66b);}}
   if(state==QUEST){header(c,"QUESTS & ACHIEVEMENTS");txt(c,"DAILY: COLLECT 15 COINS "+dayCoins+"/15",480,152,23,0xffffffff);txt(c,"WEEKLY: PLAY 10 RUNS "+weekRuns+"/10",480,208,23,0xffffffff);txt(c,"DAILY: PLAY 3 RUNS "+dayRuns+"/3",480,264,23,0xffffffff);int count=Integer.bitCount(achievements);txt(c,"ACHIEVEMENTS "+count+"/20",480,325,25,0xffffd66b);btn(c,"CLAIM COMPLETED QUESTS",290,364,380,0xff28ead7);}
   if(state==STATS){header(c,"PLAYER STATISTICS");txt(c,"BEST SCORE "+best,480,164,27,0xffffffff);txt(c,"RUNS "+runs+"  •  COINS "+totalCoins,480,230,24,0xffffffff);txt(c,"LEVEL "+level+"  •  ACHIEVEMENTS "+Integer.bitCount(achievements)+"/20",480,296,23,0xffffd66b);txt(c,"MAX COMBO THIS RUN "+maxCombo,480,355,21,0xff28ead7);}
   if(state==OPTIONS){header(c,"SETTINGS");btn(c,"FPS "+fps,170,151,280,0xffd8e3f4);btn(c,"GRAPHICS "+(quality==1?"BALANCED":"LOW"),510,151,280,0xffd8e3f4);btn(c,"MUSIC "+(music==1?"ON":"OFF"),170,233,280,0xffd8e3f4);btn(c,"SFX "+(effects==1?"ON":"OFF"),510,233,280,0xffd8e3f4);btn(c,"CONTROL "+(control==0?"DRAG":"TAP"),315,315,330,0xff28ead7);}
   if(state==CHEST){header(c,"COSMETIC CHEST");txt(c,"100 COINS = ONE RANDOM LOCKED SKIN",480,202,24,0xffffffff);txt(c,"YOUR COINS "+coins,480,264,23,0xffffd66b);btn(c,"OPEN CHEST",320,312,320,0xff28ead7);}
   if(state==TUTORIAL){box(c,225,122,735,415,0xee101b37);txt(c,"HOW TO PLAY",480,178,37,0xffffffff);txt(c,"DRAG YOUR BOT LEFT AND RIGHT",480,225,21,0xffffffff);txt(c,"AVOID RED • COLLECT GOLD",480,266,21,0xffffd66b);txt(c,"DOUBLE TAP TO DASH",480,307,21,0xff28ead7);btn(c,"LET'S PLAY",335,342,290,0xff28ead7);}
   if(state==PAUSE||state==DEAD){box(c,245,120,715,426,0xee101b37);txt(c,state==DEAD?"GAME OVER":"PAUSED",480,193,43,0xffffffff);txt(c,"SCORE "+(int)score+"  BEST "+best,480,253,23,0xffffd66b);btn(c,state==DEAD?"RETRY":"RESUME",325,281,310,0xff28ead7);btn(c,"MAIN MENU",325,354,310,0xffd8e3f4);}
   c.restore();postInvalidateDelayed(fps==30?33:16);
  }
  long tap=0;
  public boolean onTouchEvent(MotionEvent e){if(getWidth()==0||getHeight()==0)return true;float xx=e.getX()*960/getWidth(),yy=e.getY()*540/getHeight();int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){
    if(state==HOME){if(yy>282&&yy<336)start();else if(yy>348&&yy<400)state=xx<370?MODES:xx<590?SHOP:QUEST;else if(yy>411&&yy<466)state=xx<370?STATS:xx<590?CHEST:OPTIONS;}
    else if(state==PLAY){if(xx>870&&yy<70)state=PAUSE;else{long t=System.currentTimeMillis();if(t-tap<340&&dashCooldown<=0){dash=.35f;dashCooldown=5;invincible=Math.max(invincible,.35f);burst(x,y,0xff28ead7,22);}tap=t;touched=true;target=xx;}}
    else if(state==TUTORIAL){if(yy>335){seenTutorial=true;save();state=PLAY;last=System.nanoTime();}}
    else if(state==PAUSE||state==DEAD){if(yy>280&&yy<335){if(state==DEAD)start();else state=PLAY;}else if(yy>352&&yy<413)state=HOME;last=System.nanoTime();}
    else if(xx<210&&yy>450){state=HOME;}
    else if(state==MODES){if(yy>145&&yy<203){mode=Math.max(0,Math.min(2,(int)((xx-150)/224)));save();}else if(yy>270&&yy<326){int w=(int)((xx-55)/224);if(w>=0&&w<4&&level>=w*3+1){world=w;save();}}}
    else if(state==SHOP){if(yy>168&&yy<405){int row=yy<270?0:1,col=Math.round((xx-145)/135);int idx=row*6+col;if(idx>=0&&idx<12){if((unlocked&(1<<idx))!=0)skin=idx;else if(coins>=80+idx*40){coins-=80+idx*40;unlocked|=1<<idx;skin=idx;}save();}}}
    else if(state==QUEST){if(yy>360&&yy<422){rollover();if(dayCoins>=15&&(claims&1)==0){coins+=60;claims|=1;}if(dayRuns>=3&&(claims&2)==0){coins+=40;claims|=2;}if(weekRuns>=10&&(claims&4)==0){coins+=200;claims|=4;}updateAchievements();save();}}
    else if(state==CHEST){if(yy>310&&yy<375&&coins>=100){ArrayList<Integer> locked=new ArrayList<>();for(int i=1;i<12;i++)if((unlocked&(1<<i))==0)locked.add(i);if(!locked.isEmpty()){int idx=locked.get(rng.nextInt(locked.size()));coins-=100;unlocked|=1<<idx;skin=idx;save();}}}
    else if(state==OPTIONS){if(yy>150&&yy<208){if(xx<480)fps=fps==60?30:60;else quality=1-quality;}else if(yy>230&&yy<292){if(xx<480)music=1-music;else effects=1-effects;}else if(yy>310&&yy<370)control=1-control;save();}
   }else if(a==MotionEvent.ACTION_MOVE&&state==PLAY&&touched){target=control==0?xx:x+(xx-480)*.1f;}else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)touched=false;
   invalidate();return true;}
 }
}
