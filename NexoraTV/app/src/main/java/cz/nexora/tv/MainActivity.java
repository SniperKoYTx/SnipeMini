package cz.nexora.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        web = new WebView(this);
        web.setBackgroundColor(0xff080c1c);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new TvBridge(), "NexoraAndroid");
        setContentView(web);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
        web.loadUrl("file:///android_asset/index.html");
    }

    private class TvBridge {
        @JavascriptInterface public String getApps() {
            JSONArray result = new JSONArray();
            PackageManager pm = getPackageManager();
            List<ResolveInfo> apps = new ArrayList<>();
            Intent leanback = new Intent(Intent.ACTION_MAIN);
            leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            apps.addAll(pm.queryIntentActivities(leanback, 0));
            Intent regular = new Intent(Intent.ACTION_MAIN);
            regular.addCategory(Intent.CATEGORY_LAUNCHER);
            apps.addAll(pm.queryIntentActivities(regular, 0));
            Collections.sort(apps, new Comparator<ResolveInfo>() {
                @Override public int compare(ResolveInfo a, ResolveInfo b) {
                    return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
                }
            });
            Set<String> seenPackages = new HashSet<>();
            for (ResolveInfo ri: apps) {
                try {
                    if (ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(getPackageName()) || !seenPackages.add(pkg)) continue;
                    JSONObject obj = new JSONObject();
                    obj.put("name", ri.loadLabel(pm).toString());
                    obj.put("package", pkg);
                    obj.put("icon", getIcon(ri.loadIcon(pm)));
                    result.put(obj);
                    if (result.length() >= 100) break;
                } catch (Exception ignored) {}
            }
            return result.toString();
        }

        private String getIcon(Drawable drawable) {
            if (drawable == null) return "";
            try {
                Bitmap bmp = Bitmap.createBitmap(72,72,Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                drawable.setBounds(0,0,72,72);
                drawable.draw(canvas);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 80, stream);
                bmp.recycle();
                return "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) { return ""; }
        }

        @JavascriptInterface public void launchApp(final String pkg) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        PackageManager pm = getPackageManager();
                        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
                        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
                        if (intent == null) {
                            Intent query = new Intent(Intent.ACTION_MAIN);
                            query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                            query.setPackage(pkg);
                            List<ResolveInfo> list = pm.queryIntentActivities(query,0);
                            if (list.isEmpty()) {
                                query = new Intent(Intent.ACTION_MAIN);
                                query.addCategory(Intent.CATEGORY_LAUNCHER);
                                query.setPackage(pkg);
                                list = pm.queryIntentActivities(query,0);
                            }
                            if (!list.isEmpty()) {
                                ResolveInfo ri = list.get(0);
                                intent = new Intent(Intent.ACTION_MAIN);
                                intent.setComponent(new ComponentName(pkg,ri.activityInfo.name));
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            }
                        }
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this,"Aplikaci nelze spustit",Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,"Spuštění selhalo: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface public void openHomeSettings() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    try { startActivity(new Intent("android.settings.HOME_SETTINGS")); }
                    catch (Exception e) {
                        Toast.makeText(MainActivity.this,"Nastavení HOME není dostupné",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (web != null) web.evaluateJavascript(
                "window.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));", null);
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("NexoraAndroid");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
