package com.example.weather_activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private EditText editTextCity;
    private Button buttonGetWeather, buttonGetLocation;
    private TextView textViewWeather, textViewForecast;
    private ToggleButton toggleTemperature;

    private final String API_KEY = "64818fa4f88dadd7232bee593a2ce9f7";
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isCelsius = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editTextCity = findViewById(R.id.editTextCity);
        buttonGetWeather = findViewById(R.id.buttonGetWeather);
        buttonGetLocation = findViewById(R.id.buttonGetLocation);
        textViewWeather = findViewById(R.id.textViewWeather);
        textViewForecast = findViewById(R.id.textViewForecast);
        toggleTemperature = findViewById(R.id.toggleTemperature);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        toggleTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isCelsius = !isChecked; // Если включен, то °F, если выключен - °C
            toggleTemperature.setText(isCelsius ? "°C" : "°F");
            // Обновите погоду, чтобы отобразить данные в выбранной единице измерения
            String city = editTextCity.getText().toString();
            if (!city.isEmpty()) {
                getWeatherData(city);
            }
        });

        buttonGetWeather.setOnClickListener(v -> {
            String city = editTextCity.getText().toString();
            if (!city.isEmpty()) {
                getWeatherData(city);
            } else {
                Toast.makeText(MainActivity.this, "Введите город", Toast.LENGTH_SHORT).show();
            }
        });

        buttonGetLocation.setOnClickListener(v -> getLocation());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void getWeatherData(String city) {
        String units = isCelsius ? "metric" : "imperial"; // Устанавливаем единицы измерения
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY + "&units=" + units;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    displayWeatherData(response);
                    try {
                        double latitude = response.getJSONObject("coord").getDouble("lat");
                        double longitude = response.getJSONObject("coord").getDouble("lon");
                        get7DayForecast(latitude, longitude);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void get7DayForecast(double latitude, double longitude) {
        String units = isCelsius ? "metric" : "imperial"; // Устанавливаем единицы измерения
        String url = "https://api.openweathermap.org/data/2.5/onecall?lat=" + latitude + "&lon=" + longitude + "&exclude=current,minutely,hourly,alerts&appid=" + API_KEY + "&units=" + units;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray daily = response.getJSONArray("daily");
                        StringBuilder forecastBuilder = new StringBuilder("7-дневный прогноз:\n");

                        for (int i = 0; i < daily.length(); i++) {
                            JSONObject day = daily.getJSONObject(i);
                            long sunrise = day.getLong("sunrise");
                            long sunset = day.getLong("sunset");
                            double tempDay = day.getJSONObject("temp").getDouble("day");
                            double feelsLike = day.getJSONObject("feels_like").getDouble("day");
                            double humidity = day.getDouble("humidity");
                            double windSpeed = day.getDouble("wind_speed");
                            String windDirection = day.getString("wind_deg") + "°";

                            forecastBuilder.append("День ").append(i + 1).append(":\n")
                                    .append("Температура: ").append(tempDay).append(isCelsius ? "°C" : "°F").append("\n")
                                    .append("Ощущается как: ").append(feelsLike).append(isCelsius ? "°C" : "°F").append("\n")
                                    .append("Влажность: ").append(humidity).append("%\n")
                                    .append("Скорость ветра: ").append(windSpeed).append(" м/с\n")
                                    .append("Направление ветра: ").append(windDirection).append("\n")
                                    .append("Восход: ").append(convertUnixToTime(sunrise)).append("\n")
                                    .append("Закат: ").append(convertUnixToTime(sunset)).append("\n\n");
                        }

                        textViewForecast.setText(forecastBuilder.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Ошибка получения прогноза", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void displayWeatherData(JSONObject response) {
        try {
            String weatherInfo = response.getJSONArray("weather").getJSONObject(0).getString("description");
            double temperature = response.getJSONObject("main").getDouble("temp");
            double feelsLike = response.getJSONObject("main").getDouble("feels_like");
            double humidity = response.getJSONObject("main").getDouble("humidity");
            double windSpeed = response.getJSONObject("wind").getDouble("speed");
            String windDirection = response.getJSONObject("wind").getString("deg") + "°";

            textViewWeather.setText("Температура: " + temperature + (isCelsius ? "°C" : "°F") + "\n" +
                    "Ощущается как: " + feelsLike + (isCelsius ? "°C" : "°F") + "\n" +
                    "Влажность: " + humidity + "%\n" +
                    "Скорость ветра: " + windSpeed + " м/с\n" +
                    "Направление ветра: " + windDirection + "°\n" +
                    "Погода: " + weatherInfo);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Ошибка получения данных", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertUnixToTime(long unixTime) {
        java.util.Date date = new java.util.Date(unixTime * 1000);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(date);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getWeatherDataByCoordinates(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(MainActivity.this, "Не удалось получить местоположение", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getWeatherDataByCoordinates(double latitude, double longitude) {
        String units = isCelsius ? "metric" : "imperial"; // Устанавливаем единицы измерения
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY + "&units=" + units;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    displayWeatherData(response);
                    try {
                        double lat = response.getJSONObject("coord").getDouble("lat");
                        double lon = response.getJSONObject("coord").getDouble("lon");
                        get7DayForecast(lat, lon);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Разрешение на доступ к местоположению отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

