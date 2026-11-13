package com.rahat.bdadmission;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.RemoteViews;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CountdownWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "com.rahat.bdadmission.ACTION_REFRESH";
    private static final TimeZone DHAKA = TimeZone.getTimeZone("Asia/Dhaka");

    private static final char[] BN_DIGITS = {'০','১','২','৩','৪','৫','৬','৭','৮','৯'};
    private static final String[] BN_MONTHS = {
		"জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
		"জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    };
    private static final String[] BN_DAYS = {
		"", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            updateWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountdownWidgetProvider.class));
            for (int i = 0; i < ids.length; i++) {
                updateWidget(context, manager, ids[i]);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        String uniName = prefs.getString("uni_name", null);
        String unitName = prefs.getString("unit_name", null);
        String unitSub = prefs.getString("unit_sub", "");
        String examDateIso = prefs.getString("exam_date_iso", null);
        String examNote = prefs.getString("exam_note", "");
        String eventKind = prefs.getString("event_kind", "exam");
        String themeFrom = prefs.getString("theme_from", "#1e3a5f");
        String themeTo = prefs.getString("theme_to", "#0f172a");

        int colorFrom = safeColor(themeFrom, 0xFF1e3a5f);
        int colorTo = safeColor(themeTo, 0xFF0f172a);

        File campusFile = new File(context.getFilesDir(), MainActivity.CAMPUS_BG_FILE_NAME);
        Bitmap campusBitmap = campusFile.exists() ? BitmapFactory.decodeFile(campusFile.getAbsolutePath()) : null;

        Bitmap bg = createCardBitmap(720, 420, colorFrom, colorTo, 42f, campusBitmap);
        views.setImageViewBitmap(R.id.widget_bg, bg);

        File logoFile = new File(context.getFilesDir(), MainActivity.LOGO_FILE_NAME);
        if (logoFile.exists()) {
            Bitmap logo = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
            if (logo != null) {
                views.setImageViewBitmap(R.id.widget_logo, logo);
                views.setViewVisibility(R.id.widget_logo, android.view.View.VISIBLE);
            }
        } else {
            views.setViewVisibility(R.id.widget_logo, android.view.View.GONE);
        }

        if (uniName == null || unitName == null || examDateIso == null) {
            views.setTextViewText(R.id.widget_uni_name, "ভর্তি কাউন্টডাউন");
            views.setTextViewText(R.id.widget_unit_name, "অ্যাপ খুলে একটা ইউনিট বেছে নাও");
            views.setViewVisibility(R.id.widget_sub, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_exam_date, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_countdown_row, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_months_pill, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_note, android.view.View.GONE);
        } else {
            views.setTextViewText(R.id.widget_uni_name, uniName);
            views.setTextViewText(R.id.widget_unit_name, unitName);

            if (unitSub != null && !unitSub.isEmpty()) {
                views.setTextViewText(R.id.widget_sub, unitSub);
                views.setViewVisibility(R.id.widget_sub, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_sub, android.view.View.GONE);
            }

            Date target = parseIso(examDateIso);
            Date now = new Date();

            if (target == null) {
                views.setTextViewText(R.id.widget_exam_date, "নির্দিষ্ট সময় নিশ্চিত নয়");
                views.setViewVisibility(R.id.widget_exam_date, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.widget_countdown_row, android.view.View.GONE);
                views.setViewVisibility(R.id.widget_months_pill, android.view.View.GONE);
            } else {
                views.setTextViewText(R.id.widget_exam_date, formatDateLine(target));
                views.setViewVisibility(R.id.widget_exam_date, android.view.View.VISIBLE);

                long diff = target.getTime() - now.getTime();
                if (diff <= 0) {
                    views.setTextViewText(R.id.widget_days_value, "—");
                    views.setTextViewText(R.id.widget_hours_value, "—");
                    views.setTextViewText(R.id.widget_minutes_value, "—");
                    views.setTextViewText(R.id.widget_seconds_value, "—");
                    views.setTextViewText(R.id.widget_months_pill, "application".equals(eventKind) ? "আবেদনের নির্ধারিত সময় পেরিয়েছে" : "নির্ধারিত শুরুর সময় পেরিয়েছে");
                } else {
                    long days = diff / 86400000L;
                    long hours = (diff % 86400000L) / 3600000L;
                    long minutes = (diff % 3600000L) / 60000L;
                    long seconds = (diff % 60000L) / 1000L;

                    views.setTextViewText(R.id.widget_days_value, toBn(days));
                    views.setTextViewText(R.id.widget_hours_value, toBn(hours));
                    views.setTextViewText(R.id.widget_minutes_value, toBn(minutes));
                    views.setTextViewText(R.id.widget_seconds_value, "—");
                    views.setTextViewText(R.id.widget_months_pill, monthsAndDaysText(now, target));
                }
                views.setViewVisibility(R.id.widget_countdown_row, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.widget_months_pill, android.view.View.VISIBLE);
            }

            if (examNote != null && !examNote.isEmpty()) {
                views.setTextViewText(R.id.widget_note, examNote);
                views.setViewVisibility(R.id.widget_note, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_note, android.view.View.GONE);
            }
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
			context, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentImmutableFlag());
        views.setOnClickPendingIntent(R.id.widget_root, openPending);

        Intent refreshIntent = new Intent(context, CountdownWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(
			context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentImmutableFlag());
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending);

        manager.updateAppWidget(widgetId, views);
    }

    private int pendingIntentImmutableFlag() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        }
        return 0;
    }

    private int safeColor(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private Bitmap createCardBitmap(int widthPx, int heightPx, int colorFrom, int colorTo, float cornerRadiusPx, Bitmap campusBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, widthPx, heightPx);
        clipPath.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
        canvas.clipPath(clipPath);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(colorTo);
        canvas.drawRect(rect, bgPaint);

        if (campusBitmap != null) {
            Bitmap cropped = centerCrop(campusBitmap, widthPx, heightPx);
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ColorMatrix saturation = new ColorMatrix();
            saturation.setSaturation(0.45f);
            ColorMatrix brightness = new ColorMatrix(new float[]{
														 0.6f, 0, 0, 0, 0,
														 0, 0.6f, 0, 0, 0,
														 0, 0, 0.6f, 0, 0,
														 0, 0, 0, 1, 0
													 });
            saturation.postConcat(brightness);
            imagePaint.setColorFilter(new ColorMatrixColorFilter(saturation));
            imagePaint.setAlpha(190);
            canvas.drawBitmap(cropped, 0, 0, imagePaint);
        }

        Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int fadedFrom = (campusBitmap != null) ? (colorFrom & 0x00FFFFFF) | 0xB0000000 : colorFrom;
        int fadedTo = (campusBitmap != null) ? (colorTo & 0x00FFFFFF) | 0xB0000000 : colorTo;
        LinearGradient gradient = new LinearGradient(
			0, 0, widthPx, heightPx, fadedFrom, fadedTo, Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradient);
        canvas.drawRect(rect, gradientPaint);

        return bitmap;
    }

    private Bitmap centerCrop(Bitmap source, int targetWidth, int targetHeight) {
        float sourceRatio = (float) source.getWidth() / source.getHeight();
        float targetRatio = (float) targetWidth / targetHeight;

        int srcWidth, srcHeight, srcX, srcY;
        if (sourceRatio > targetRatio) {
            srcHeight = source.getHeight();
            srcWidth = Math.round(srcHeight * targetRatio);
            srcX = (source.getWidth() - srcWidth) / 2;
            srcY = 0;
        } else {
            srcWidth = source.getWidth();
            srcHeight = Math.round(srcWidth / targetRatio);
            srcX = 0;
            srcY = (source.getHeight() - srcHeight) / 2;
        }
        srcX = Math.max(0, srcX);
        srcY = Math.max(0, srcY);
        srcWidth = Math.min(srcWidth, source.getWidth() - srcX);
        srcHeight = Math.min(srcHeight, source.getHeight() - srcY);

        Bitmap cropped = Bitmap.createBitmap(source, srcX, srcY, srcWidth, srcHeight);
        Matrix matrix = new Matrix();
        matrix.postScale((float) targetWidth / srcWidth, (float) targetHeight / srcHeight);
        return Bitmap.createBitmap(cropped, 0, 0, srcWidth, srcHeight, matrix, true);
    }

    private Date parseIso(String iso) {
        if (iso == null || iso.length() == 0) return null;
        // New web payloads always carry an explicit +06:00 offset.
        String value = iso;
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        if (iso.matches(".*[+-][0-9]{2}:[0-9]{2}$")) {
            value = iso.substring(0, iso.length() - 3) + iso.substring(iso.length() - 2);
            pattern += "Z";
        } else if (iso.endsWith("Z")) {
            value = iso.substring(0, iso.length() - 1) + "+0000";
            pattern += "Z";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
        sdf.setLenient(false);
        sdf.setTimeZone(DHAKA);
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        Date parsed = sdf.parse(value, pos);
        return pos.getIndex() == value.length() ? parsed : null;
    }

    private String formatDateLine(Date date) {
        Calendar cal = Calendar.getInstance(DHAKA);
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int weekday = cal.get(Calendar.DAY_OF_WEEK);
        return toBn(day) + " " + BN_MONTHS[month] + ", " + toBn(year) + " • " + BN_DAYS[weekday];
    }

    private String monthsAndDaysText(Date now, Date target) {
        Calendar calNow = Calendar.getInstance(DHAKA);
        calNow.setTime(now);
        Calendar calTarget = Calendar.getInstance(DHAKA);
        calTarget.setTime(target);

        int months = (calTarget.get(Calendar.YEAR) - calNow.get(Calendar.YEAR)) * 12
			+ (calTarget.get(Calendar.MONTH) - calNow.get(Calendar.MONTH));
        int days = calTarget.get(Calendar.DAY_OF_MONTH) - calNow.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            months -= 1;
            Calendar prevMonth = (Calendar) calTarget.clone();
            prevMonth.add(Calendar.MONTH, -1);
            int prevMonthLastDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            days += prevMonthLastDay;
        }
        if (months < 0) months = 0;
        if (days < 0) days = 0;

        if (months > 0) {
            return toBn(months) + " মাস " + toBn(days) + " দিন বাকি";
        }
        return toBn(days) + " দিন বাকি";
    }

    private String toBn(long n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(BN_DIGITS[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
