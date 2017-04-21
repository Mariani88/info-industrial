package com.altsoft.seba.geolocalizacion;


        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.api.GoogleApiClient;
        import com.google.android.gms.location.LocationServices;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;
        import com.google.android.gms.maps.model.Polygon;
        import com.google.android.gms.maps.model.PolygonOptions;

        import android.Manifest;
        import android.app.NotificationManager;
        import android.content.pm.PackageManager;
        import android.graphics.Color;
        import android.location.Location;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Handler;
        import android.provider.Settings;
        import android.support.annotation.NonNull;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.support.v7.app.NotificationCompat;
        import android.widget.Toast;

        import java.util.LinkedList;
        import java.util.List;

        import static android.R.attr.id;

/**
 * This demo shows how GMS Location can be used to check for changes to the users location.  The
 * "My Location" button uses GMS Location to set the blue dot representing the users location.
 * Permission for {@link android.Manifest.permission#ACCESS_FINE_LOCATION} is requested at run
 * time. If the permission has not been granted, the Activity is finished with an error message.
 */
public class MyLocationDemoActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnPolygonClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private LatLng puntoB = new LatLng(-34.5829, -58.3850);
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private PolygonOptions poligono;
    private List<LatLng> puntos = new LinkedList<LatLng>();
    private Integer contador = 0;
    private Handler handler = new Handler();
    private Runnable timedTask = new Runnable(){
        int count = 0;
        @Override
        public void run() {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                Double lat = mLastLocation.getLatitude();
                Double lon = mLastLocation.getLongitude();
                LatLng miUbicacion = new LatLng(lat,lon);
                if (poligono != null) {
                    if (miUbicacionEstaDentroDelPoligono(miUbicacion, poligono)) {
                        Toast.makeText(getApplicationContext(), "dentro", Toast.LENGTH_SHORT).show();
                    } else {
                        count++;
                        Toast.makeText(getApplicationContext(), "fuera", Toast.LENGTH_SHORT).show();
                        if (count%15 == 0 || count == 1) {
                            notification1(id, "Fuera", "El objetivo ha salido del area de interes");
                        }
                    }
                }
            }
            handler.postDelayed(timedTask, 4000);
        }
    };


    public void notification1(int id, String titulo, String contenido) {

        NotificationCompat.Builder builder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                        .setContentTitle(titulo)
                        .setContentText(contenido)
                        .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                        .setLights(Color.RED, 3000, 3000)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        ;

        // Construir la notificación y emitirla
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.notify(id, builder.build());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        map.setOnMapClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnPolygonClickListener(this);
        enableMyLocation();
        //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);  vista de mapa satelital
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(puntoB));
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        handler.removeCallbacks(timedTask);
        mMap.clear();
        return false;
    }

    private boolean miUbicacionEstaDentroDelPoligono(LatLng miUbicacion, PolygonOptions poligono) {
        contador = 0;
        LatLng p1,p2;
        for (int i=0; i < poligono.getPoints().size()-1; i++){
            p1 = poligono.getPoints().get(i);
            p2 = poligono.getPoints().get(i+1);
            if (sonValidos(p1,p2,miUbicacion)){
                if (cortaLaRecta(p1, p2, miUbicacion)) {
                    contador++;
                }
            }
        }
        p1 = poligono.getPoints().get(poligono.getPoints().size()-1);
        p2 = poligono.getPoints().get(0);
        if (sonValidos(p1,p2,miUbicacion)){
            if (cortaLaRecta(p1, p2, miUbicacion)) {
                contador++;
            }
        }
        return (contador%2 != 0);
    }

    private boolean sonValidos(LatLng p1, LatLng p2, LatLng miUbicacion) {
        Double miLat = miUbicacion.latitude;
        Double miLong = miUbicacion.longitude;
        return !((p1.longitude < miLong && p2.longitude < miLong)||
                (p1.latitude > miLat  && p2.latitude > miLat)||(p1.latitude < miLat && p2.latitude < miLat));
    }

    private boolean cortaLaRecta(LatLng p1, LatLng p2, LatLng miUbicacion) {
        Double ejex = ((miUbicacion.latitude - p1.latitude)/(p2.latitude-p1.latitude)*(p2.longitude-p1.longitude))+p1.longitude;
        return (ejex > miUbicacion.longitude);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        } else {
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.clear();
        if (!puntos.isEmpty()){
            crearPoligono(puntos);
        }
        puntos.clear();
        return true;
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPolygonClick(Polygon polygon) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
        handler.removeCallbacks(timedTask);
        crearMarcador(latLng);
    }


    private void crearMarcador(LatLng punto){
        if (puntos.isEmpty()){
            mMap.clear();
        }
        MarkerOptions marcador = new MarkerOptions().position(punto).title(" ");
        marcador.draggable(true);
        mMap.addMarker(marcador);
        puntos.add(marcador.getPosition());
    }

    private void crearPoligono(List<LatLng> puntos){
        poligono = new PolygonOptions()
                .addAll(puntos);
        Polygon polygon = mMap.addPolygon(poligono);
        polygon.setStrokeWidth(3);
        handler.post(timedTask);
    }
}
