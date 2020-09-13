package fr.vocality.gpstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.types.ObjectId;

import fr.vocality.gpstracker.beans.Track;
import fr.vocality.gpstracker.dialogs.CreateTrackDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection, CreateTrackDialog.CreateTrackDialogListener {
    private static final String TAG = "MainActivity";

    private Button btnStartLocation;
    private TextView lblLocation;
    private TextView lblLastUpdate;
    private TextView lblServerStatus;

    private boolean mTrackingLocation;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private Track mCurrentTrack;

    private BroadcastReceiver br;
    private ForegroundServiceLocation serviceLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "[onCreate()]");

        initUI();
        initBroadcastReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart() ");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");
        Log.d(TAG, "[onDestroy()] calling unregisterReceiver....");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mnuInflater = getMenuInflater();
        mnuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_db:
                Intent intent = new Intent(getApplicationContext(), DbActivity.class);
                /*
                final ArrayList<Location> locations = serviceLocation.readLocationsFromLocalDb();
                final Bundle bundle = new Bundle();
                bundle.putBinder("LOCATIONS_ARRAY_LIST", new ObjectWrapperForBinder(locations));
                intent.putExtras(bundle);
                 */

                startActivity(intent);
                return true;

            case R.id.action_quit:
                if (mTrackingLocation) {
                    stopService(new Intent(getApplicationContext(), ForegroundServiceLocation.class));
                    unbindService(this);
                }
                finish();
                return true;

            case R.id.action_settings:
                if (mTrackingLocation) {
                    stopService(new Intent(getApplicationContext(), ForegroundServiceLocation.class));
                    unbindService(this);
                }
                Intent intent1 = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent1);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initUI() {
        androidx.appcompat.widget.Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        lblServerStatus = findViewById(R.id.lblServerStatus);
        lblLocation = findViewById(R.id.lblLocation);
        lblLastUpdate = findViewById(R.id.lblLastUpdate);

        btnStartLocation = findViewById(R.id.btnStartLocation);
        btnStartLocation.setOnClickListener(this);
    }

    private void initBroadcastReceiver() {
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "[onReceive()] action: " + intent.getAction() + " - timestamp: " + intent.getStringExtra("timestamp"));
                //Log.d(TAG, "[onReceive()] action: " + intent.getAction() + " - latitude: " + intent.getDoubleExtra("latitude", 0));
                //Log.d(TAG, "[onReceive()] action: " + intent.getAction() + " - longitude: " + intent.getDoubleExtra("longitude", 0));

                String locStr = String.format("Latitude: %s\nLongitude: %s", intent.getDoubleExtra("latitude", 0), intent.getDoubleExtra("longitude", 0));

                lblLastUpdate.setText(intent.getStringExtra("timestamp"));
                lblLocation.setText(locStr);
                lblServerStatus.setText(intent.getStringExtra("server_status"));
            }
        };

        IntentFilter filter = new IntentFilter("fr.vocality.gpstracker.LOCATION_NOTIFICATION");
        LocalBroadcastManager.getInstance(this).registerReceiver(br, filter);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnStartLocation:
                if (! mTrackingLocation) {
                    createNewTrack();
                } else {
                    stopTrackingLocation(v);
                }
                break;

            default:
                break;
        }
    }

    public Track getCurrentTrack() {
        return mCurrentTrack;
    }

    private void createNewTrack() {
        ObjectId trackId = new ObjectId();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String timestamp = sdf.format(new Date());

        mCurrentTrack = new Track();
        mCurrentTrack.setId(trackId.toString());
        mCurrentTrack.setName("track_" + trackId);
        mCurrentTrack.setStartTime(timestamp);

        Log.d(TAG, mCurrentTrack.toString());

        // open dialog
        CreateTrackDialog dialog = new CreateTrackDialog(this);
        dialog.show(getSupportFragmentManager(), "fr.vocality.gpstracker.dialogs.NewTrackDialog");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogPositiveClick: " + mCurrentTrack.toString());
        startTrackingLocation(getWindow().getDecorView().getRootView());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogNegativeClick: " + mCurrentTrack.toString());
        startTrackingLocation(getWindow().getDecorView().getRootView());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //startTrackingLocation();
                Toast.makeText(this, "Permission granted !", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startTrackingLocation(View v) {
        Log.d(TAG, "startTrackingLocation: ");
        mTrackingLocation = true;
        btnStartLocation.setText(R.string.stop_tracking);

        Intent intent = new Intent(v.getContext(), ForegroundServiceLocation.class);
        intent.putExtra("currentTrack", mCurrentTrack);
        // TODO: intent.putExtra("user, mUser)
        startService(intent);

        Log.d(TAG, "startTrackingLocation: calling bindService....");
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    private void stopTrackingLocation(View v) {
        Log.d(TAG, "stopTrackingLocation: ");

        if (mTrackingLocation) {
            mTrackingLocation = false;
            btnStartLocation.setText(R.string.start_tracking);
            lblLastUpdate.setText("--");
            lblLocation.setText("--");
            lblServerStatus.setText("--");

            stopService(new Intent(v.getContext(), ForegroundServiceLocation.class));
            unbindService(this);
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ForegroundServiceLocation.MyBinder b = (ForegroundServiceLocation.MyBinder) service;
        serviceLocation = b.getService();
        Log.d(TAG, "[onServiceConnected()] Connected to ForegroundServiceLocation !");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        serviceLocation = null;
        Log.d(TAG, "onServiceDisconnected: ");
    }
}