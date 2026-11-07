package com.example.prayertimes;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private TextView tvHijriDate, tvNextPrayerName, tvCountdown;
    private TextView tvFajr, tvSunrise, tvDhuhr, tvAsr, tvMaghrib, tvIsha;

    private final Map<String, String> prayerTimings = new HashMap<>();
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // إحداثيات الموقع (القدس الشريف)
    private final double latitude = 31.7683;
    private final double longitude = 35.2137;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بناء الواجهة كاملة برمجياً بدون الحاجة لملفات XML أو R
        buildUserInterface();

        // جلب المواقيت
        fetchPrayerTimes(latitude, longitude);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void buildUserInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#0B3C39"));
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(20), dp(24), dp(20), dp(24));
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        scrollView.addView(mainLayout);

        // عنوان التطبيق
        TextView tvTitle = new TextView(this);
        tvTitle.setText("مواقيت الصلاة");
        tvTitle.setTextColor(Color.parseColor("#D4AF37"));
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        mainLayout.addView(tvTitle);

        // التاريخ الهجري
        tvHijriDate = new TextView(this);
        tvHijriDate.setText("جاري تحميل الأوقات...");
        tvHijriDate.setTextColor(Color.parseColor("#E0E0E0"));
        tvHijriDate.setTextSize(14);
        tvHijriDate.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        dateParams.setMargins(0, dp(4), 0, dp(20));
        mainLayout.addView(tvHijriDate, dateParams);

        // بطاقة الصلاة القادمة
        LinearLayout nextCard = new LinearLayout(this);
        nextCard.setOrientation(LinearLayout.VERTICAL);
        nextCard.setBackgroundColor(Color.parseColor("#154F4B"));
        nextCard.setPadding(dp(16), dp(18), dp(16), dp(18));
        nextCard.setGravity(Gravity.CENTER);

        TextView tvNextTitle = new TextView(this);
        tvNextTitle.setText("الصلاة القادمة");
        tvNextTitle.setTextColor(Color.parseColor("#CCCCCC"));
        tvNextTitle.setTextSize(14);
        nextCard.addView(tvNextTitle);

        tvNextPrayerName = new TextView(this);
        tvNextPrayerName.setText("--");
        tvNextPrayerName.setTextColor(Color.WHITE);
        tvNextPrayerName.setTextSize(30);
        tvNextPrayerName.setTypeface(null, Typeface.BOLD);
        nextCard.addView(tvNextPrayerName);

        tvCountdown = new TextView(this);
        tvCountdown.setText("00:00:00");
        tvCountdown.setTextColor(Color.parseColor("#D4AF37"));
        tvCountdown.setTextSize(22);
        tvCountdown.setTypeface(null, Typeface.BOLD);
        nextCard.addView(tvCountdown);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(20));
        mainLayout.addView(nextCard, cardParams);

        // قائمة الصلوات
        tvFajr = addPrayerRow(mainLayout, "الفجر");
        tvSunrise = addPrayerRow(mainLayout, "الشروق");
        tvDhuhr = addPrayerRow(mainLayout, "الظهر");
        tvAsr = addPrayerRow(mainLayout, "العصر");
        tvMaghrib = addPrayerRow(mainLayout, "المغرب");
        tvIsha = addPrayerRow(mainLayout, "العشاء");

        setContentView(scrollView);
    }

    private TextView addPrayerRow(LinearLayout parent, String prayerName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#124844"));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView tvName = new TextView(this);
        tvName.setText(prayerName);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        row.addView(tvName, nameParams);

        TextView tvTime = new TextView(this);
        tvTime.setText("--:--");
        tvTime.setTextColor(Color.parseColor("#D4AF37"));
        tvTime.setTextSize(16);
        tvTime.setTypeface(null, Typeface.BOLD);
        row.addView(tvTime);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(8));
        parent.addView(row, rowParams);

        return tvTime;
    }

    private void fetchPrayerTimes(double lat, double lng) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.aladhan.com/v1/timings?latitude=" + lat + "&longitude=" + lng + "&method=4";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject data = jsonResponse.getJSONObject("data");
                JSONObject timings = data.getJSONObject("timings");
                JSONObject hijri = data.getJSONObject("date").getJSONObject("hijri");

                String hijriDateStr = hijri.getString("day") + " " +
                        hijri.getJSONObject("month").getString("ar") + " " +
                        hijri.getString("year") + " هـ";

                prayerTimings.put("Fajr", timings.getString("Fajr"));
                prayerTimings.put("Sunrise", timings.getString("Sunrise"));
                prayerTimings.put("Dhuhr", timings.getString("Dhuhr"));
                prayerTimings.put("Asr", timings.getString("Asr"));
                prayerTimings.put("Maghrib", timings.getString("Maghrib"));
                prayerTimings.put("Isha", timings.getString("Isha"));

                runOnUiThread(() -> {
                    tvHijriDate.setText(hijriDateStr);
                    tvFajr.setText(formatTo12Hour(prayerTimings.get("Fajr")));
                    tvSunrise.setText(formatTo12Hour(prayerTimings.get("Sunrise")));
                    tvDhuhr.setText(formatTo12Hour(prayerTimings.get("Dhuhr")));
                    tvAsr.setText(formatTo12Hour(prayerTimings.get("Asr")));
                    tvMaghrib.setText(formatTo12Hour(prayerTimings.get("Maghrib")));
                    tvIsha.setText(formatTo12Hour(prayerTimings.get("Isha")));

                    startCountdown();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "فشل في تحديث المواقيت، تحقق من اتصالك", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void startCountdown() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateNextPrayerAndTimer();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateNextPrayerAndTimer() {
        if (prayerTimings.isEmpty()) return;

        String[] prayers = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] prayerArabicNames = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

        Calendar now = Calendar.getInstance();
        long minDiff = Long.MAX_VALUE;
        String nextPrayer = "";

        for (int i = 0; i < prayers.length; i++) {
            String timeStr = prayerTimings.get(prayers[i]);
            if (timeStr == null) continue;

            String[] parts = timeStr.split(":");
            Calendar prayerCal = Calendar.getInstance();
            prayerCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            prayerCal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            prayerCal.set(Calendar.SECOND, 0);

            long diff = prayerCal.getTimeInMillis() - now.getTimeInMillis();
            if (diff < 0) {
                prayerCal.add(Calendar.DAY_OF_YEAR, 1);
                diff = prayerCal.getTimeInMillis() - now.getTimeInMillis();
            }

            if (diff < minDiff) {
                minDiff = diff;
                nextPrayer = prayerArabicNames[i];
            }
        }

        long hours = minDiff / (1000 * 60 * 60);
        long minutes = (minDiff % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (minDiff % (1000 * 60)) / 1000;

        tvNextPrayerName.setText(nextPrayer);
        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private String formatTo12Hour(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", new Locale("ar"));
            Date date = sdf24.parse(time24);
            return (date != null) ? sdf12.format(date) : time24;
        } catch (Exception e) {
            return time24;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }
}