package com.example.hikerswatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;
    Location loc = null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0 ){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,1,locationListener);
                }
            }
        }
    }

    public class DownloadElevation extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {
            try{
                URL url = new URL(strings[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data;
                String json = "";
                do{
                    data = reader.read();
                    char c = (char) data;
                    json += c;
                }while(data!=-1);
                JSONObject jsonObject = new JSONObject(json);
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                jsonObject = jsonArray.getJSONObject(0);
                String elevation = jsonObject.getString("elevation");
                elevation = String.format("%.2f",Double.parseDouble(elevation));
                Log.i("Elevation", elevation);
                return elevation;
            }catch(Exception e){
                e.printStackTrace();
                return "failed";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                loc = location;
                Button button = findViewById(R.id.button);
                getLocation(button);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,1,locationListener);
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
        }
    }

    public void getLocation(View view){
        if(loc == null){
            Toast.makeText(this, "No location data", Toast.LENGTH_SHORT).show();
        }else {
            TextView latText = findViewById(R.id.latTextView);
            latText.setText("Latitude: " + String.format("%.5f", loc.getLatitude()));
            TextView longText = findViewById(R.id.longTextView);
            longText.setText("Longitude: " + String.format("%.5f", loc.getLongitude()));
            TextView accText = findViewById(R.id.accTextView);
            accText.setText("Accuracy: " + String.valueOf(loc.getAccuracy()));
            TextView eleText = findViewById(R.id.altiTextView);
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            DownloadElevation dl = new DownloadElevation();
            try {
                List<Address> listOfAddresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (listOfAddresses != null && listOfAddresses.size() > 0) {
                    String address = "";
                    Log.i("Address", listOfAddresses.get(0).toString());
                    if (listOfAddresses.get(0).getAddressLine(0) != null) {
                        address += listOfAddresses.get(0).getAddressLine(0);
                    }
                    TextView addressText = findViewById(R.id.addressTextView);
                    addressText.setText(address);
                    String geturl = getString(R.string.api) + loc.getLatitude() + ","
                            + loc.getLongitude() + getString(R.string.key);
                    Log.i("URL", geturl);
                    String ele = dl.execute(geturl).get();
                    eleText.setText("Elevation: " + ele + "m");
                }
            } catch (Exception e) {
                //e.printStackTrace();
                Log.i("Failed", "Didn't connect");
            }
            Toast.makeText(getApplicationContext(), loc.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}