package com.rahat.bdadmission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String SITE_URL = "https://bd-admission.vercel.app/";
    private static final String SITE_HOST = "bd-admission.vercel.app";
    public static final String PREFS_NAME = "bd_admission_widget";
    public static final String LOGO_FILE_NAME = "widget_logo.png";
    public static final String CAMPUS_BG_FILE_NAME = "widget_campus_bg.jpg";
    private static final long LOAD_TIMEOUT_MS = 15000;

    private boolean mainFrameFailed;
    private WebView webView;
    private FrameLayout loadingOverlay;
    private FrameLayout errorOverlay;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable loadTimeoutRunnable;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyFullscreen();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#0f172a"));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0f172a"));
        root.addView(webView, new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        loadingOverlay = buildLoadingOverlay();
        root.addView(loadingOverlay, new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        errorOverlay = buildErrorOverlay();
        errorOverlay.setVisibility(View.GONE);
        root.addView(errorOverlay, new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new WidgetBridge(), "Android");

        webView.setWebViewClient(new WebViewClient() {
				@Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (isTrustedUrl(url)) return false;
                    openExternal(url);
                    return true;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return shouldOverrideUrlLoading(view, request.getUrl().toString());
                }

				@Override
				public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
					handler.cancel();
                    mainFrameFailed = true;
                    cancelLoadTimeout();
                    showError();
				}

				@Override
				public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
					super.onPageStarted(view, url, favicon);
                    mainFrameFailed = false;
					showLoading();
					startLoadTimeout();
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
                    if (mainFrameFailed) return;
					cancelLoadTimeout();
					showContent();
				}

				@Override
				public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
					super.onReceivedError(view, request, error);
					if (request.isForMainFrame()) {
                        mainFrameFailed = true;
						cancelLoadTimeout();
						showError();
					}
				}

				@Override
				@SuppressWarnings("deprecation")
				public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
					super.onReceivedError(view, errorCode, description, failingUrl);
					if (isTrustedUrl(failingUrl)) {
                        mainFrameFailed = true;
						cancelLoadTimeout();
						showError();
					}
				}
			});

        webView.loadUrl(SITE_URL);
        checkForUpdate();
    }

    private void checkForUpdate() {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						URL url = new URL("https://bd-admission.vercel.app/api/apk-url");
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setInstanceFollowRedirects(false);
                        conn.setConnectTimeout(8000);
						conn.setReadTimeout(8000);
						InputStream in = conn.getInputStream();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int n;
						while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
						in.close();
						final JSONObject data = new JSONObject(bos.toString("UTF-8"));
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									handleUpdateResponse(data);
								}
							});
					} catch (Exception e) {
					}
				}
			}).start();
    }

    private void handleUpdateResponse(JSONObject data) {
        int latestVersionCode = data.optInt("versionCode", -1);
        String latestVersionName = data.optString("versionName", "");
        String changelog = data.optString("changelog", "");
        String apkUrl = data.optString("url", "");

        if (latestVersionCode <= 0 || apkUrl.isEmpty()) return;
        if (latestVersionCode <= getCurrentVersionCode()) return;

        showUpdateDialog(latestVersionName, changelog, apkUrl);
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void showUpdateDialog(String versionName, String changelog, final String apkUrl) {
        String message = changelog.replace("\\n", "\n");
        if (message.isEmpty()) message = "A new version is available with improvements and fixes.";

        String title = versionName.isEmpty() ? "Update Available" : "Update Available (v" + versionName + ")";

        new AlertDialog.Builder(this)
			.setTitle(title)
			.setMessage(message)
			.setCancelable(true)
			.setPositiveButton("Update", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openExternal(apkUrl);
				}
			})
			.setNegativeButton("Later", null)
			.show();
    }

    private void startLoadTimeout() {
        cancelLoadTimeout();
        loadTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                mainFrameFailed = true;
                webView.stopLoading();
                showError();
            }
        };
        timeoutHandler.postDelayed(loadTimeoutRunnable, LOAD_TIMEOUT_MS);
    }

    private void cancelLoadTimeout() {
        if (loadTimeoutRunnable != null) {
            timeoutHandler.removeCallbacks(loadTimeoutRunnable);
            loadTimeoutRunnable = null;
        }
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        errorOverlay.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);
    }

    private void showError() {
        loadingOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
    }

    private FrameLayout buildLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#0f172a"));
        ProgressBar spinner = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        overlay.addView(spinner, params);
        return overlay;
    }

    private FrameLayout buildErrorOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#0f172a"));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("লোড করা যায়নি");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("ইন্টারনেট কানেকশন চেক করো, তারপর আবার চেষ্টা করো।");
        subtitle.setTextColor(Color.parseColor("#93c5fd"));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(20));

        Button retryButton = new Button(this);
        retryButton.setText("আবার চেষ্টা করো");
        retryButton.setTextColor(Color.WHITE);
        retryButton.setBackgroundColor(Color.parseColor("#3b82f6"));
        retryButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLoading();
					startLoadTimeout();
					webView.loadUrl(SITE_URL);
				}
			});

        box.addView(title);
        box.addView(subtitle);
        box.addView(retryButton);

        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        boxParams.gravity = Gravity.CENTER;
        overlay.addView(box, boxParams);
        return overlay;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private boolean isTrustedUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return "https".equalsIgnoreCase(uri.getScheme())
            && SITE_HOST.equalsIgnoreCase(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443);
    }

    private void openExternal(String url) {
        if (url == null) return;
        Uri uri = Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, "লিংক খোলার জন্য একটি ব্রাউজার প্রয়োজন", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void applyFullscreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.parseColor("#0b1325"));
        getWindow().setNavigationBarColor(Color.parseColor("#0b1325"));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    protected void onDestroy() {
        cancelLoadTimeout();
        if (webView != null) {
            webView.removeJavascriptInterface("Android");
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class WidgetBridge {

        @JavascriptInterface
        public synchronized void saveSelection(String payloadJson) {
            try {
                JSONObject data = new JSONObject(payloadJson);

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("uni_name", data.optString("uniName", ""));
                editor.putString("unit_name", data.optString("unitName", ""));
                editor.putString("unit_sub", data.optString("unitSub", ""));
                editor.putString("exam_date_iso", data.optString("examDateIso", ""));
                editor.putString("exam_note", data.optString("examNote", ""));
                editor.putString("event_kind", data.optString("eventKind", "exam"));

                JSONObject theme = data.optJSONObject("theme");
                if (theme != null) {
                    editor.putString("theme_from", theme.optString("headerFrom", "#1e3a5f"));
                    editor.putString("theme_to", theme.optString("headerTo", "#0f172a"));
                    editor.putString("theme_accent", theme.optString("accent", "#3b82f6"));
                }
                editor.apply();

                String logoUrl = data.optString("uniLogo", "");
                String cachedLogoUrl = prefs.getString("logo_url_cached", "");
                if (!isTrustedUrl(logoUrl)) logoUrl = "";
                if (logoUrl.isEmpty()) {
                    new File(getFilesDir(), LOGO_FILE_NAME).delete();
                    editor.putString("logo_url_cached", "").apply();
                }
                if (!logoUrl.isEmpty() && !logoUrl.equals(cachedLogoUrl)) {
                    editor.putString("logo_url_cached", logoUrl);
                    editor.apply();
                    downloadFileAndRefresh(logoUrl, LOGO_FILE_NAME, "logo_url_cached");
                }

                String campusBgUrl = data.optString("campusBg", "");
                if (!isTrustedUrl(campusBgUrl)) campusBgUrl = "";
                String cachedCampusUrl = prefs.getString("campus_url_cached", "");
                File campusFile = new File(getFilesDir(), CAMPUS_BG_FILE_NAME);
                if (!campusBgUrl.isEmpty()) {
                    if (!campusBgUrl.equals(cachedCampusUrl)) {
                        editor.putString("campus_url_cached", campusBgUrl);
                        editor.apply();
                        downloadFileAndRefresh(campusBgUrl, CAMPUS_BG_FILE_NAME, "campus_url_cached");
                    }
                } else {
                    if (campusFile.exists()) campusFile.delete();
                    editor.putString("campus_url_cached", "");
                    editor.apply();
                }

                refreshWidget();
            } catch (Exception e) {
            }
        }

        private void downloadFileAndRefresh(final String fileUrl, final String fileName, final String cacheKeyPref) {
            new Thread(new Runnable() {
					@Override
					public void run() {
						boolean success = false;
						try {
							URL url = new URL(fileUrl);
							HttpURLConnection conn = (HttpURLConnection) url.openConnection();
							conn.setInstanceFollowRedirects(false);
                        conn.setConnectTimeout(8000);
							conn.setReadTimeout(8000);
							InputStream in = conn.getInputStream();
							Bitmap bitmap = BitmapFactory.decodeStream(in);
							in.close();
							if (bitmap != null) {
                                synchronized (WidgetBridge.this) {
                                    SharedPreferences current = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                    if (!fileUrl.equals(current.getString(cacheKeyPref, ""))) return;
                                    File outFile = new File(getFilesDir(), fileName);
                                    FileOutputStream out = new FileOutputStream(outFile);
                                    try {
                                        Bitmap.CompressFormat format = fileName.endsWith(".png")
                                            ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                                        bitmap.compress(format, 90, out);
                                    } finally { out.close(); }
                                }
								success = true;
							}
						} catch (Exception e) {
						}
						if (!success) {
							SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
							synchronized (WidgetBridge.this) {
                                if (fileUrl.equals(prefs.getString(cacheKeyPref, "")))
                                    prefs.edit().putString(cacheKeyPref, "").apply();
                            }
						}
						refreshWidget();
					}
				}).start();
        }

        private void refreshWidget() {
            Intent updateIntent = new Intent(MainActivity.this, CountdownWidgetProvider.class);
            updateIntent.setAction("com.rahat.bdadmission.ACTION_REFRESH");
            sendBroadcast(updateIntent);
        }
    }
}
