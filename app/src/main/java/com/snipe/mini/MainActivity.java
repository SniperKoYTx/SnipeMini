package com.snipe.mini;
import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
 DigView game;
 @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);game=new DigView(this);setContentView(game);}
 @Override protected void onPause(){if(game!=null)game.save();super.onPause();}
 @Override public void onBackPressed(){if(game!=null){game.save();game.menu=!game.menu;game.invalidate();}}
 static class DigView extends View {
  final Paint p=new Paint(3);final Random rnd=new Random();final SharedPreferences prefs;
  static final int COLS=22,ROWS=34;final byte[][] sand=new byte[COLS][ROWS];final float[] treasureX={.25f,.70f,.50f,.27f,.76f,.51f},treasureY={.18f,.29f,.44f,.62f,.73f,.89f};
  final String[] treasureNames={"ANCIENT COIN","ROBOT HAND","LOST ENGINE","METAL FACE","POWER CORE","ROBOT HEART"};
  final int[] treasureColors={0xffffd66c,0xff8ec7c8,0xffb5a1df,0xffa3b4b9,0xff65f4f0,0xffff7e66};
  boolean[] found=new boolean[6];int coins=0,level=1,zone=1,cleared=0;boolean menu=false;float brushX=-1,brushY=-1;long lastDig=0;int messageTimer=0;String message="";
  DigView(Context c){super(c);prefs=c.getSharedPreferences("unburied_v1",0);load();setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  void load(){coins=prefs.getInt("coins",0);level=prefs.getInt("level",1);zone=prefs.getInt("zone",1);String map=prefs.getString("map","");if(map.length()==COLS*ROWS){for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++)sand[x][y]=(byte)(map.charAt(y*COLS+x)-'0');}else resetMap();for(int i=0;i<6;i++)found[i]=prefs.getBoolean("f"+i,false);recount();}
  void resetMap(){for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++)sand[x][y]=3;cleared=0;Arrays.fill(found,false);}
  void recount(){cleared=0;for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++)if(sand[x][y]==0)cleared++;}
  void save(){StringBuilder s=new StringBuilder(COLS*ROWS);for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++)s.append((char)('0'+sand[x][y]));SharedPreferences.Editor e=prefs.edit().putInt("coins",coins).putInt("level",level).putInt("zone",zone).putString("map",s.toString());for(int i=0;i<6;i++)e.putBoolean("f"+i,found[i]);e.apply();}
  void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
  void text(Canvas c,String s,float x,float y,float size,int col){color(col);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
  void box(Canvas c,float l,float t,float r,float b,int col,float rad){color(col);c.drawRoundRect(l,t,r,b,rad,rad,p);}
  void robot(Canvas c,float cx,float cy,float scale){c.save();c.translate(cx,cy);c.scale(scale,scale);box(c,-60,-78,60,67,0xff546977,16);box(c,-52,-69,52,28,0xff8fa7a9,14);box(c,-39,-35,-9,-15,0xff44e5e4,5);box(c,9,-35,39,-15,0xff44e5e4,5);box(c,-18,0,18,7,0xff2e444f,4);box(c,-42,38,42,54,0xff314e58,6);box(c,-85,-40,-61,45,0xff778b93,9);box(c,61,-40,85,45,0xff778b93,9);box(c,-29,67,-8,100,0xff526b7c,5);box(c,8,67,29,100,0xff526b7c,5);c.restore();}
  void discover(){for(int i=0;i<6;i++){if(found[i])continue;int cx=(int)(treasureX[i]*COLS),cy=(int)(treasureY[i]*ROWS);int clear=0,total=0;for(int yy=Math.max(0,cy-2);yy<=Math.min(ROWS-1,cy+2);yy++)for(int xx=Math.max(0,cx-2);xx<=Math.min(COLS-1,cx+2);xx++){total++;if(sand[xx][yy]==0)clear++;}if(clear>=total*.8f){found[i]=true;int reward=25+i*15;coins+=reward;message="DISCOVERED: "+treasureNames[i]+" +"+reward;messageTimer=110;save();}}}
  void dig(float xx,float yy){if(menu)return;float w=getWidth(),h=getHeight(),top=h*.16f,bottom=h*.83f;if(yy<top||yy>bottom)return;int cx=(int)(xx/w*COLS),cy=(int)((yy-top)/(bottom-top)*ROWS);int radius=level==1?1:level==2?2:3;boolean changed=false;for(int y=Math.max(0,cy-radius);y<=Math.min(ROWS-1,cy+radius);y++)for(int x=Math.max(0,cx-radius);x<=Math.min(COLS-1,cx+radius);x++){float d=(x-cx)*(x-cx)+(y-cy)*(y-cy);if(d>radius*radius+1)continue;if(sand[x][y]>0){sand[x][y]--;if(sand[x][y]==0){cleared++;coins++;}changed=true;}}brushX=xx;brushY=yy;if(changed){discover();invalidate();if(System.currentTimeMillis()-lastDig>1200){save();lastDig=System.currentTimeMillis();}}}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),top=h*.16f,bottom=h*.83f,cellW=w/COLS,cellH=(bottom-top)/ROWS;c.drawColor(0xff102534);
   color(0xff183c4d);for(int i=0;i<45;i++){float x=(i*73%100)*w/100f,y=(i*41%100)*h/100f;c.drawCircle(x,y,1+i%3,p);}
   text(c,"UNBURIED",w/2,h*.066f,w*.092f,0xffffd991);text(c,"DIG SITE "+zone+"  |  "+(cleared*100/(COLS*ROWS))+"% EXCAVATED",w/2,h*.108f,w*.037f,0xffd4e6e6);
   box(c,w*.04f,h*.122f,w*.96f,h*.146f,0xff31505b,8);box(c,w*.04f,h*.122f,w*(.04f+.92f*cleared/(COLS*ROWS)),h*.146f,0xff56d7bd,8);
   box(c,0,top,w,bottom,0xff3a302b,0);robot(c,w*.5f,top+(bottom-top)*.59f,w/370f);
   for(int i=0;i<6;i++){float tx=treasureX[i]*w,ty=top+treasureY[i]*(bottom-top);color(treasureColors[i]);c.drawCircle(tx,ty,w*.052f,p);if(found[i])text(c,"✓",tx,ty+w*.014f,w*.05f,0xff142936);}
   for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){int v=sand[x][y];if(v==0)continue;int base=(x*19+y*11+zone*13)%17;int col=v==3?Color.rgb(207+base,155+base/2,83+base/2):v==2?Color.rgb(192+base,138+base/2,75+base/2):Color.rgb(170+base,119+base/2,68+base/2);color(col);c.drawRect(x*cellW,top+y*cellH,(x+1)*cellW+1,top+(y+1)*cellH+1,p);}
   if(brushX>=0){color(0x55ffffff);c.drawCircle(brushX,brushY,w*(level==1?.055f:level==2?.075f:.105f),p);}
   box(c,0,bottom,w,h,0xff172f3c,0);text(c,"COINS "+coins,w*.25f,h*.88f,w*.047f,0xffffd991);text(c,"TOOL LV."+level,w*.75f,h*.88f,w*.047f,0xff83e6da);
   int price=level==1?90:level==2?250:600;box(c,w*.035f,h*.905f,w*.475f,h*.975f,0xff53d7b8,14);text(c,level>=4?"MAX TOOL":"UPGRADE "+price,w*.255f,h*.951f,w*.037f,0xff102a34);
   box(c,w*.525f,h*.905f,w*.965f,h*.975f,0xffefc77c,14);text(c,"NEXT DIG SITE",w*.745f,h*.951f,w*.036f,0xff26303a);
   if(messageTimer>0){messageTimer--;box(c,w*.04f,h*.49f,w*.96f,h*.57f,0xdd173845,14);text(c,message,w*.5f,h*.538f,w*.035f,0xffffffff);postInvalidateDelayed(35);}
   if(menu){box(c,w*.08f,h*.32f,w*.92f,h*.64f,0xee102735,20);text(c,"PAUSED",w*.5f,h*.41f,w*.075f,0xffffffff);text(c,"TAP TO CONTINUE",w*.5f,h*.52f,w*.046f,0xffffd991);}
  }
  @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){if(menu){menu=false;invalidate();return true;}if(y>h*.89f){if(x<w*.5f){int price=level==1?90:level==2?250:600;if(level<4&&coins>=price){coins-=price;level++;message="TOOL UPGRADED!";messageTimer=65;}else{message=level>=4?"MAX LEVEL":"NEED "+price+" COINS";messageTimer=55;}}else{if(cleared>COLS*ROWS*.65f){zone++;resetMap();message="NEW DIG SITE UNLOCKED";messageTimer=90;}else{message="EXCAVATE 65% TO ADVANCE";messageTimer=70;}}save();invalidate();return true;}}dig(x,y);return true;}if(a==MotionEvent.ACTION_MOVE){dig(x,y);return true;}if(a==MotionEvent.ACTION_UP){brushX=-1;brushY=-1;save();invalidate();return true;}return true;}
 }
}
