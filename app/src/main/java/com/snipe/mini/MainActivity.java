package com.snipe.mini;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  TextView text = new TextView(this);
  text.setText("Hello Snipe!");
  text.setTextSize(28);
  text.setGravity(Gravity.CENTER);
  setContentView(text);
 }
}
