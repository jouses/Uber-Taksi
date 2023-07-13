package com.jouse.uber_taksi.Main;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.jouse.uber_taksi.Data.JsonData;
import com.jouse.uber_taksi.Giris.GirisActivity;
import com.jouse.uber_taksi.R;
import com.jouse.uber_taksi.databinding.ActivityMainBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    FirebaseAuth auth;
    private GoogleMap mMap;
    ActivityMainBinding binding;
    LocationManager locationManager;
    LocationListener locationListener;
    LatLng userLocation = null;
    ActivityResultLauncher<String> permissionLauncher;
    boolean firstZoom;
    ArrayList<JsonData> jsonDataArrayList;
    ArrayList<Polyline> polylines;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        jsonDataArrayList = new ArrayList<>();
        polylines = new ArrayList<>();
        registerLauncher();
        getJson();
        firstZoom = false;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        findViewById(R.id.openMenuIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.cikisMenu){
                    auth.signOut();
                    startActivity(new Intent(MainActivity.this,GirisActivity.class));
                }
                return true;
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                if(!firstZoom){
                    LatLng loc = new LatLng(location.getLatitude(),location.getLongitude());
                    if(userLocation != null && !userLocation.equals(loc)){
                        userLocation = loc;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,13));
                    }
                    firstZoom = true;
                }

                for(int i = 0; i < jsonDataArrayList.size(); i++){
                    float[] result = new float[1];
                    Location.distanceBetween(location.getLatitude(),location.getLongitude(),jsonDataArrayList.get(i).latitude,jsonDataArrayList.get(i).longitude,result);
                    int finalresult = (int) result[0];
                    jsonDataArrayList.get(i).mesafe = finalresult;
                }

            }
        };

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.getRoot(),"Konum izni gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction("İzin ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }).show();
            }
            else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        else {
            permissionOk();
        }
    }

    private void direction(LatLng startPosition,LatLng endPosition){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = Uri.parse("https://maps.googleapis.com/maps/api/directions/json")
                .buildUpon()
                .appendQueryParameter("destination", startPosition.latitude + ", " + startPosition.longitude)
                .appendQueryParameter("origin", endPosition.latitude + ", " + endPosition.longitude)
                .appendQueryParameter("mode", "driving")
                .appendQueryParameter("key", "AIzaSyCerbVdVcFPCYnxqeCYFQEl7BN6vVee5D4")
                .toString();
        System.out.println(url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("OK")) {
                        JSONArray routes = response.getJSONArray("routes");

                        ArrayList<LatLng> points;

                        for (int i=0;i<routes.length();i++){
                            points = new ArrayList<>();
                            JSONArray legs = routes.getJSONObject(i).getJSONArray("legs");

                            for (int j=0;j<legs.length();j++){
                                JSONArray steps = legs.getJSONObject(j).getJSONArray("steps");

                                for (int k=0;k<steps.length();k++){
                                    float startLatitude = Float.parseFloat(steps.getJSONObject(k).getJSONObject("start_location").getString("lat"));
                                    float startLongitude = Float.parseFloat(steps.getJSONObject(k).getJSONObject("start_location").getString("lng"));
                                    LatLng startLatLng = new LatLng(startLatitude,startLongitude);

                                    float endLatitude = Float.parseFloat(steps.getJSONObject(k).getJSONObject("end_location").getString("lat"));
                                    float endLongitude = Float.parseFloat(steps.getJSONObject(k).getJSONObject("end_location").getString("lng"));
                                    LatLng endLatLang = new LatLng(endLatitude,endLongitude);

                                    polylines.add(mMap.addPolyline(new PolylineOptions().add(startLatLng,endLatLang)));
                                    System.out.println(startLatLng + " " + endLatLang);
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RetryPolicy retryPolicy = new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);
    }


    public void permissionOk(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastLocation != null){
                LatLng lastLocationLatLng = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                userLocation = lastLocationLatLng;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng,13));
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            for(int i = 0; i<jsonDataArrayList.size(); i++){
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(jsonDataArrayList.get(i).latitude,jsonDataArrayList.get(i).longitude));
                markerOptions.snippet(jsonDataArrayList.get(i).marka);
                if(jsonDataArrayList.get(i).uber){
                    markerOptions.title("Uber");
                }
                else {
                    markerOptions.title("Taksi");
                }

                switch (jsonDataArrayList.get(i).marka){
                    case "taksi":
                        markerOptions.icon(bitmapDescriptor(getApplicationContext(),R.drawable.taksi));
                        break;
                    case "volvo":
                        markerOptions.icon(bitmapDescriptor(getApplicationContext(),R.drawable.volvo));
                        break;
                    case "ford":
                        markerOptions.icon(bitmapDescriptor(getApplicationContext(),R.drawable.ford));
                        break;
                    case "mercedes":
                        markerOptions.icon(bitmapDescriptor(getApplicationContext(),R.drawable.mercedes));
                        break;
                }

                mMap.addMarker(markerOptions);
            }
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onInfoWindowClick(@NonNull Marker marker) {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                    View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.map_bottom_sheet,null);
                    bottomSheetDialog.setContentView(bottomSheetView);
                    bottomSheetView.findViewById(R.id.bottomSheetCloseButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            bottomSheetDialog.dismiss();
                        }
                    });
                    bottomSheetView.findViewById(R.id.yolTarifiButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            binding.directionLayout.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.direction_layout_enter_anim));
                            if(polylines.size() != 0){
                                for (Polyline polyline : polylines){
                                    polyline.remove();
                                }
                            }
                            direction(userLocation,marker.getPosition());
                            bottomSheetDialog.dismiss();
                            binding.targetDirectionText.setText(marker.getTitle());
                            binding.directionLayout.setVisibility(View.VISIBLE);
                            TextView carMesafe = bottomSheetView.findViewById(R.id.carMesafeText);
                            binding.directionMetreText.setText(carMesafe.getText());
                            binding.directionRemoveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    binding.directionLayout.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.direction_layout_exit_anim));
                                    for (Polyline polyline : polylines){
                                        polyline.remove();
                                    }
                                    binding.directionLayout.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    });
                    bottomSheetDialog.show();
                    TextView carText = bottomSheetView.findViewById(R.id.carText);
                    TextView carMesafe = bottomSheetView.findViewById(R.id.carMesafeText);
                    TextView taksiUberTopText = bottomSheetView.findViewById(R.id.taksiUberTopText);
                    ImageView carImage = bottomSheetView.findViewById(R.id.carImage);
                    for (int i = 0; i < jsonDataArrayList.size(); i++){
                        LatLng selectedMarkerPosition = marker.getPosition();
                        LatLng arrayListMarkerPosition = new LatLng(jsonDataArrayList.get(i).latitude,jsonDataArrayList.get(i).longitude);
                        if(selectedMarkerPosition.equals(arrayListMarkerPosition)){
                            carText.setText(jsonDataArrayList.get(i).marka);
                            carMesafe.setText(jsonDataArrayList.get(i).mesafe + " metre");
                            taksiUberTopText.setText(marker.getTitle());

                            switch (jsonDataArrayList.get(i).marka){
                                case "taksi":
                                    carImage.setImageResource(R.drawable.taksi);
                                    break;
                                case "volvo":
                                    carImage.setImageResource(R.drawable.volvo);
                                    break;
                                case "ford":
                                    carImage.setImageResource(R.drawable.ford);
                                    break;
                                case "mercedes":
                                    carImage.setImageResource(R.drawable.mercedes);
                                    break;
                            }
                        }
                    }
                }
            });
        }
    }

    public void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    permissionOk();
                }
            }
        });
    }

    public void getJson(){
        String json;
        try {
            InputStream is = getAssets().open("Data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JsonData jsonData = new JsonData(jsonObject.getBoolean("Uber"),jsonObject.getString("Marka"),jsonObject.getDouble("latitude"),jsonObject.getDouble("longitude"),0);
                jsonDataArrayList.add(jsonData);
            }

        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("NonConstantResourceId")
    private BitmapDescriptor bitmapDescriptor(Context context, int vectorId){
        Drawable drawble = ContextCompat.getDrawable(context,vectorId);
        switch (vectorId){
            case R.drawable.taksi:
                drawble.setBounds(0,0,150,120);
                break;
            case R.drawable.volvo:
                drawble.setBounds(0,0,150,80);
                break;
            case R.drawable.ford:
            case R.drawable.mercedes:
                drawble.setBounds(0,0,150,100);
                break;
        }
        Bitmap bitmap = Bitmap.createBitmap(150,150,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawble.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}