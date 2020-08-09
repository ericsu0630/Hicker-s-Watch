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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0 ){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,1,locationListener);
                }
            }
        }
    }

    public static class DownloadElevation extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {
            try{
                URL url = new URL(strings[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data;
                StringBuilder json = new StringBuilder();
                do{
                    data = reader.read();
                    char c = (char) data;
                    json.append(c);
                }while(data!=-1);
                JSONObject jsonObject = new JSONObject(json.toString());
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                jsonObject = jsonArray.getJSONObject(0);
                String elevation = jsonObject.getString("elevation");
                elevation = String.format(Locale.getDefault(),"%.2f", Double.parseDouble(elevation));
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
                Log.i("Location",location.toString());
                getLocation(location);
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,1,locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastKnownLocation == null){
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(lastKnownLocation == null){
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if(lastKnownLocation == null){
                        lastKnownLocation = new Location("DEFAULT");
                        lastKnownLocation.setLatitude(25.033964);
                        lastKnownLocation.setLongitude(121.564468);

                    }
                }
            }
            Log.i("Last Known Location", lastKnownLocation.toString());
            getLocation(lastKnownLocation);
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
        }
    }

    public void getLocation(Location loc){
        TextView latText, longText, accText, eleText;
        latText = findViewById(R.id.latTextView);
        longText = findViewById(R.id.longTextView);
        accText = findViewById(R.id.accTextView);
        eleText = findViewById(R.id.altiTextView);
        latText.setText(getString(R.string.latitude) + String.format(Locale.getDefault(),"%.5f", loc.getLatitude()));
        longText.setText(getString(R.string.longitude) + String.format(Locale.getDefault(),"%.5f", loc.getLongitude()));
        accText.setText(getString(R.string.accuracy) + String.format(Locale.getDefault(),"%.2f", loc.getAccuracy()));

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
                eleText.setText(getString(R.string.altitude) + ele);
                Toast.makeText(getApplicationContext(), "Location updated", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Failed", "Didn't connect");
            Toast.makeText(getApplicationContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
        }
    }
}