package com.gfd.cropwis.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.gfd.cropwis.models.Weather5Day;
import com.gfd.cropwis.utils.Formatting;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import com.gfd.cropwis.R;
import com.gfd.cropwis.tasks.ParseResult;
import com.gfd.cropwis.utils.UnitConvertor;

public class GraphActivity extends BaseActivity {

    SharedPreferences sp;

    int theme;

    ArrayList<Weather5Day> weather5DayList = new ArrayList<>();

    float minTemp = 100000;
    float maxTemp = 0;

    float minRain = 100000;
    float maxRain = 0;

    float minPressure = 100000;
    float maxPressure = 0;

    float minWindSpeed = 100000;
    float maxWindSpeed = 0;

    private String labelColor = "#000000";
    private String lineColor = "#333333";

    private boolean darkTheme = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(theme = getTheme(prefs.getString("theme", "fresh")));
        darkTheme = theme == R.style.AppTheme_NoActionBar_Dark ||
                theme == R.style.AppTheme_NoActionBar_Black ||
                theme == R.style.AppTheme_NoActionBar_Classic_Dark ||
                theme == R.style.AppTheme_NoActionBar_Classic_Black;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        Toolbar toolbar = findViewById(R.id.graph_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView temperatureTextView = findViewById(R.id.graphTemperatureTextView);
        TextView rainTextView = findViewById(R.id.graphRainTextView);
        TextView pressureTextView = findViewById(R.id.graphPressureTextView);
        TextView windSpeedTextView = findViewById(R.id.graphWindSpeedTextView);

        if (darkTheme) {
            toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay_Dark);
            labelColor = "#FFFFFF";
            lineColor = "#FAFAFA";

            temperatureTextView.setTextColor(Color.parseColor(labelColor));
            rainTextView.setTextColor(Color.parseColor(labelColor));
            pressureTextView.setTextColor(Color.parseColor(labelColor));
            windSpeedTextView.setTextColor(Color.parseColor(labelColor));
        }

        sp = PreferenceManager.getDefaultSharedPreferences(GraphActivity.this);
        String lastLongterm = sp.getString("lastLongterm", "");
        String lastLongterm16Days = sp.getString("lastLongterm16Days", "");

        if (parseLongTerm16DaysJson(lastLongterm16Days) == ParseResult.OK) {
            temperatureGraph();
            rainGraph();
            pressureGraph();
            windSpeedGraph();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.msg_err_parsing_json, Snackbar.LENGTH_LONG).show();
        }
    }

    private void temperatureGraph() {
        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_temperature);

        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weather5DayList.size(); i++) {
            float temperature = UnitConvertor.convertTemperature(Float.parseFloat(weather5DayList.get(i).getTemperature()), sp);

            if (temperature < minTemp) {
                minTemp = temperature;
            }

            if (temperature > maxTemp) {
                maxTemp = temperature;
            }

            dataset.addPoint(getDateLabel(weather5DayList.get(i), i), temperature);
        }
        dataset.setSmooth(false);
        dataset.setColor(Color.parseColor("#FF5722"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor(lineColor));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) (Math.round(minTemp)) - 1, (int) (Math.round(maxTemp)) + 1);
        lineChartView.setStep(2);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);
        lineChartView.setLabelsColor(Color.parseColor(labelColor));

        lineChartView.show();
    }

    private void rainGraph() {
        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_rain);

        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weather5DayList.size(); i++) {
            float rain = Float.parseFloat(weather5DayList.get(i).getRain());

            if (rain < minRain) {
                minRain = rain;
            }

            if (rain > maxRain) {
                maxRain = rain;
            }

            dataset.addPoint(getDateLabel(weather5DayList.get(i), i), rain);
        }
        dataset.setSmooth(false);
        dataset.setColor(Color.parseColor("#2196F3"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor(lineColor));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues(0, (int) (Math.round(maxRain)) + 1);
        lineChartView.setStep(1);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);
        lineChartView.setLabelsColor(Color.parseColor(labelColor));

        lineChartView.show();
    }

    private void pressureGraph() {
        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_pressure);

        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weather5DayList.size(); i++) {
            float pressure = UnitConvertor.convertPressure(Float.parseFloat(weather5DayList.get(i).getPressure()), sp);

            if (pressure < minPressure) {
                minPressure = pressure;
            }

            if (pressure > maxPressure) {
                maxPressure = pressure;
            }

            dataset.addPoint(getDateLabel(weather5DayList.get(i), i), pressure);
        }
        dataset.setSmooth(true);
        dataset.setColor(Color.parseColor("#4CAF50"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor(lineColor));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) minPressure - 1, (int) maxPressure + 1);
        lineChartView.setStep(2);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);
        lineChartView.setLabelsColor(Color.parseColor(labelColor));

        lineChartView.show();
    }

    private void windSpeedGraph() {
        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_windspeed);
        String graphLineColor = "#efd214";

        if (darkTheme) {
            graphLineColor = "#FFF600";
        }

        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weather5DayList.size(); i++) {
            float windSpeed = (float) UnitConvertor.convertWind(Float.parseFloat(weather5DayList.get(i).getWind()), sp);

            if (windSpeed < minWindSpeed) {
                minWindSpeed = windSpeed;
            }

            if (windSpeed > maxWindSpeed) {
                maxWindSpeed = windSpeed;
            }

            dataset.addPoint(getDateLabel(weather5DayList.get(i), i), windSpeed);
        }
        dataset.setSmooth(false);
        dataset.setColor(Color.parseColor(graphLineColor));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor(lineColor));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) minWindSpeed - 1, (int) maxWindSpeed + 1);
        lineChartView.setStep(2);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);
        lineChartView.setLabelsColor(Color.parseColor(labelColor));

        lineChartView.show();
    }


    public ParseResult parseLongTermJson(String result) {
        int i;
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                return ParseResult.CITY_NOT_FOUND;
            }

            JSONArray list = reader.getJSONArray("list");
            for (i = 0; i < list.length(); i++) {
                Weather5Day weather5Day = new Weather5Day();

                JSONObject listItem = list.getJSONObject(i);
                JSONObject main = listItem.getJSONObject("main");

                JSONObject windObj = listItem.optJSONObject("wind");
                weather5Day.setWind(windObj.getString("speed"));

                weather5Day.setPressure(main.getString("pressure"));
                weather5Day.setHumidity(main.getString("humidity"));

                JSONObject rainObj = listItem.optJSONObject("rain");
                JSONObject snowObj = listItem.optJSONObject("snow");
                if (rainObj != null) {
                    weather5Day.setRain(MainActivity.getRainString(rainObj));
                } else {
                    weather5Day.setRain(MainActivity.getRainString(snowObj));
                }

                weather5Day.setDate(listItem.getString("dt"));
                weather5Day.setTemperature(main.getString("temp"));

                weather5DayList.add(weather5Day);
            }
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    public ParseResult parseLongTerm16DaysJson(String result) {
        int i;
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                if (weather5DayList == null) {
                    weather5DayList = new ArrayList<>();
                }
                return ParseResult.CITY_NOT_FOUND;
            }

            weather5DayList = new ArrayList<>();

            JSONArray list = reader.getJSONArray("list");
            for (i = 0; i < list.length(); i++) {
                Weather5Day weather = new Weather5Day();

                JSONObject listItem = list.getJSONObject(i);

                weather.setDate(listItem.getString("dt"));

                float kelvinTemp = UnitConvertor.celsiusToKelvin(Float.parseFloat(listItem.getJSONObject("temp").getString("day")));
                weather.setTemperature(String.valueOf(kelvinTemp));
                weather.setDescription(listItem.optJSONArray("weather").getJSONObject(0).getString("description"));
                weather.setWind(listItem.getString("speed"));
                weather.setWindDirectionDegree(listItem.optDouble("deg"));

                weather.setPressure(listItem.getString("pressure"));
                weather.setHumidity(listItem.getString("humidity"));
                if (listItem.has("rain")) {
                    weather.setRain(listItem.getString("rain"));
                } else {
                    weather.setRain("0");
                }


                final String idString = listItem.optJSONArray("weather").getJSONObject(0).getString("id");
                weather.setId(idString);

                final String dateMsString = listItem.getString("dt") + "000";
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(Long.parseLong(dateMsString));
                Formatting formatting = new Formatting(GraphActivity.this);
                weather.setIcon(formatting.setWeatherIcon(Integer.parseInt(idString), cal.get(Calendar.HOUR_OF_DAY)));

                weather5DayList.add(weather);
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GraphActivity.this).edit();
            editor.putString("lastLongterm16Days", result);
            editor.commit();
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    String previous = "";

    public String getDateLabel(Weather5Day weather5Day, int i) {
        if ((i + 4) % 4 == 0) {
            SimpleDateFormat resultFormat = new SimpleDateFormat("E");
            resultFormat.setTimeZone(TimeZone.getDefault());
            String output = resultFormat.format(weather5Day.getDate());
            if (!output.equals(previous)) {
                previous = output;
                return output;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private int getTheme(String themePref) {
        switch (themePref) {
            case "dark":
                return R.style.AppTheme_NoActionBar_Dark;
            case "black":
                return R.style.AppTheme_NoActionBar_Black;
            case "classic":
                return R.style.AppTheme_NoActionBar_Classic;
            case "classicdark":
                return R.style.AppTheme_NoActionBar_Classic_Dark;
            case "classicblack":
                return R.style.AppTheme_NoActionBar_Classic_Black;
            default:
                return R.style.AppTheme_NoActionBar;
        }
    }
}
