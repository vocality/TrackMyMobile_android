package fr.vocality.gpstracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.vocality.gpstracker.beans.Track;
import fr.vocality.gpstracker.utils.DbUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import io.realm.Realm;

public class ForegroundServiceLocation extends Service {

    ////////////////////////////////////////////
    // IMPORTANT: Change URL for production !
    //
    private static final String REMOTE_URL = "http://192.168.43.128:8000/tracking";
    //private static final String REMOTE_URL = "https://trackmymobile.vocality.fr/tracking";
    // TODO: remove userId mock
    private String userId = "5f2697cc69b07f2ff81fc279";

    private static final String URL_PATH_ADD_TRACK = "/addTrack";
    private static final String URL_PATH_ADD_USER = "/addUser";
    private static final String URL_PATH_ADD_DEVICE_LOCATION = "/addDeviceLocation";
    private static final String TAG = "FGServiceLocation";

    private NotificationManager mNotificationManager;

    private OkHttpClient client;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    //private String remoteServerUrl;
    private String httpStatus;

    private boolean isRemoteServerEnabled, isLocalSaveEnabled;

    private String username;
    private String jsonLocation;
    private SimpleDateFormat sdf;

    private boolean mTrackingLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private long locationRequestInterval;
    private static final int LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private LocationCallback mLocationCallback;

    private Realm mRealm;
    private Track mCurrentTrack;

    private Intent intentUpdateUI;
    private PendingIntent resultIntent;
    private IBinder mBinder = new MyBinder();

    @SuppressLint("LongLogTag")
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "[onBind()]...");
        return mBinder;
    }

    public class MyBinder extends Binder {
        ForegroundServiceLocation getService() {
            return ForegroundServiceLocation.this;
        }
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();

        // prefs
        handlePrefs();

        // init http client
        initHttpClient();
        Realm.init(this);
        sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mNotificationManager = getSystemService(NotificationManager.class);
        mLocationCallback = new LocationCallback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIdx = locationResult.getLocations().size() - 1;
                    double latitude = locationResult.getLocations().get(latestLocationIdx).getLatitude();
                    double longitude = locationResult.getLocations().get(latestLocationIdx).getLongitude();
                    long time = locationResult.getLocations().get(latestLocationIdx).getTime();
                    String timestamp = sdf.format(new Date(time));
                    float accuracy = locationResult.getLocations().get(latestLocationIdx).getAccuracy();

                    Log.d(TAG, "[onCreate() - onLocationResult()] - timestamp " + timestamp + " - " + String.format("Latitude: %s\nLongitude: %s\nAccuracy: %f", latitude, longitude, accuracy) );

                    doWorkflowLocationChanged(latitude, longitude, timestamp, accuracy, true);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: check if intent extras not null
        // get currentTrack from intent
        mCurrentTrack = (Track) intent.getSerializableExtra("currentTrack");
        postCurrentTrack(mCurrentTrack, userId);

        startTrackingLocation();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.addCategory(Intent. CATEGORY_LAUNCHER ) ;
        notificationIntent.setAction(Intent. ACTION_MAIN ) ;
        notificationIntent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP );
        resultIntent = PendingIntent. getActivity (getApplicationContext(), 0 , notificationIntent, 0 ) ;

        Notification notification = new Notification.Builder(this)
                .setShowWhen(true)
                .setContentIntent(resultIntent)
                .setContentTitle("GPS Tracker")
                .setSmallIcon(R.drawable.ic_car_location_notification)
                .setOngoing(true).build();

        startForeground(1001, notification);

        return  START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        stopTrackingLocation();
    }

    private void handlePrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if ( ! prefs.contains("location_req_interval")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("location_req_interval", "15"); // default = 15 minutes
            editor.commit();
        }

        locationRequestInterval = Long.parseLong(prefs.getString("location_req_interval", "")) * 60 * 1000;
        //remoteServerUrl = prefs.getString("server_url", "");
        isRemoteServerEnabled = prefs.getBoolean("save_remote", false);
        isLocalSaveEnabled = prefs.getBoolean("save_local", false);
        username = prefs.getString("username", "n/a");

        //Toast.makeText(this, String.format("LocReqInt: %s\nUrl: %s\nsaveRemote: %b\nsaveLocal: %b", locationRequestInterval, remoteServerUrl, isRemoteServerEnabled, isLocalSaveEnabled), Toast.LENGTH_LONG).show();
    }

    private void doWorkflowLocationChanged(double latitude, double longitude, String timestamp, float accuracy, boolean isSavedToDb) {

        ////////////////////////////////
        // update MainActivity UI
        ////////////////////////////////
        intentUpdateUI = new Intent();
        intentUpdateUI.setAction("fr.vocality.gpstracker.LOCATION_NOTIFICATION");
        intentUpdateUI.putExtra("latitude", latitude);
        intentUpdateUI.putExtra("longitude", longitude);
        intentUpdateUI.putExtra("timestamp", timestamp);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentUpdateUI);

        /////////////////////////
        // update notification
        ////////////////////////
        Notification notification = new Notification.Builder(ForegroundServiceLocation.this)
                .setShowWhen(true)
                .setContentTitle("GPS Tracker")
                .setContentText("last update: " + timestamp)
                .setContentIntent(resultIntent)
                .setStyle(new Notification.BigTextStyle()
                        .bigText("last update: " + timestamp + "\n" + String.format("Latitude: %s\nLongitude: %s", latitude, longitude)))
                .setSmallIcon(R.drawable.ic_car_location_notification)
                .setOngoing(true).build();
        mNotificationManager.notify(1001, notification);

        Log.d(TAG, "[doWorkflowLocationChanged()] - mCurrentTrack: " + mCurrentTrack.toString());

        ///////////////////////////////
        // save location to local DB
        // realm (mongo)
        ///////////////////////////////
        if (isLocalSaveEnabled && isSavedToDb) {
            //DbUtils.saveLocationToLocalDb(latitude, longitude, timestamp, Realm.getDefaultInstance());
            DbUtils.saveLocationToLocalDb(latitude, longitude, timestamp, mCurrentTrack.getId(), Realm.getDefaultInstance());
        }

        ////////////////////////////////////
        // post current location to server
        ////////////////////////////////////
        if (isRemoteServerEnabled && isSavedToDb) {
            jsonLocation = "{\"longitude\":" + longitude + ","
                    + "\"latitude\":" + latitude + ","
                    + "\"timestamp\":" + "\"" + timestamp + "\","
                    + "\"track_id\":" + "\"" + mCurrentTrack.getId() + "\","
                    + "\"user_id\":" + "\"" + userId + "\","
                    + "\"accuracy\":" + accuracy + " }";

            try {
                postHttp(REMOTE_URL + URL_PATH_ADD_DEVICE_LOCATION, jsonLocation);
            } catch (IOException ioexc) {
                ioexc.printStackTrace();
            }
        }
    }

    private void postCurrentTrack(Track mCurrentTrack, String userId) {
        Log.d(TAG, "[postCurrentTrack()]...");
        String jsonCurrentTrack = "{\"track_id\":" + "\"" + mCurrentTrack.getId() + "\","
                + "\"track_name\":" + "\"" + mCurrentTrack.getName() + "\","
                + "\"start_time\":" + "\"" + mCurrentTrack.getStartTime() + "\","
                + "\"user_id\":" + "\"" + userId + "\" }";
        try {
            postHttp(REMOTE_URL + URL_PATH_ADD_TRACK, jsonCurrentTrack);
        } catch (IOException ioexc) {
            ioexc.printStackTrace();
        }
    }

    private void initHttpClient() {
        client = new OkHttpClient();
    }

    private void postHttp(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                call.cancel();
                Log.d("TAG", "[postHttp()] - onFailure()...");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (final ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code: " + response);
                    final String myResponse = responseBody.string();
                    //Log.d("body", myResponse);
                    JSONObject json = new JSONObject(myResponse);
                    httpStatus = json.getString("status");

                    // update 'server status' UI
                    Log.d(TAG, "[postHttp()] - onResponse() - httpStatus: " + httpStatus);
                    if (intentUpdateUI == null) {
                        intentUpdateUI = new Intent();
                        intentUpdateUI.setAction("fr.vocality.gpstracker.LOCATION_NOTIFICATION");
                        Log.d(TAG, "onResponse: create new Intent...");
                    }
                    intentUpdateUI.putExtra("server_status", httpStatus);
                    LocalBroadcastManager.getInstance(ForegroundServiceLocation.this).sendBroadcast(intentUpdateUI);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();

        long offset = 0;
        if (locationRequestInterval == 60000) { // 1mn
            offset = 20 * 1000; //20s
        } else if (locationRequestInterval == 300000) { // 5mns
            offset = 160 * 1000; //2mn40 TODO (add +1mn/no battery saver)
        } else if (locationRequestInterval == 600000) { // 10mns
            offset = 320 * 1000; //5mn20 TODO (add +2mns/no battery saver)
        } else if (locationRequestInterval == 900000) { // 15mns
            //offset = 460 * 1000; //7mn40 (add +3mns20/no battery saver)
            offset = 820 * 1000;
        }
        locationRequestInterval += offset;

        locationRequest.setInterval(locationRequestInterval);
        locationRequest.setFastestInterval(locationRequestInterval / 2);
        locationRequest.setPriority(LOCATION_REQUEST_PRIORITY);
        //Log.d(TAG, "getLocationRequest: locationReqInterval: " + locationRequest.getInterval());
        //Log.d(TAG, "getLocationRequest: locationFastestInterval:" + locationRequest.getFastestInterval());
        return locationRequest;
    }

    @SuppressLint({"MissingPermission", "LongLogTag"})
    private void startTrackingLocation() {
        Log.d(TAG, "[startTrackingLocation()]...");
        mTrackingLocation = true;
        getLastKnownLocation(false);
        fusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, null);
    }

    @SuppressLint("LongLogTag")
    private void stopTrackingLocation() {
        Log.d(TAG, "[stopTrackingLocation:()]...");

        if (mTrackingLocation) {
            mTrackingLocation = false;
        }
        fusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation(final boolean isSavedToDb) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<android.location.Location>() {
                    @Override
                    public void onSuccess(android.location.Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            float accuracy = location.getAccuracy();
                            long time = location.getTime();
                            String timestamp = sdf.format(new Date(time));

                            doWorkflowLocationChanged(latitude, longitude, timestamp, accuracy, isSavedToDb);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "[getLastKnownLocation()] onFailure(): Error trying to get last GPS location !");
                        e.printStackTrace();
                    }
                });

    }
}
