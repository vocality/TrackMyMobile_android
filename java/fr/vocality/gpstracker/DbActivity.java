package fr.vocality.gpstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import fr.vocality.gpstracker.beans.Location;
import fr.vocality.gpstracker.dialogs.ClearLocalDbDialog;
import fr.vocality.gpstracker.utils.DbUtils;
import io.realm.Realm;

public class DbActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DbActivity";

    private RecyclerView recyclerView;
    private LocationAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private Button btnExportDb;
    private Button btnClearDb;
    private ArrayList<Location> locations;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db);

        // init realm db
        Realm.init(this);
        mRealm = Realm.getDefaultInstance();
        locations = DbUtils.readLocationsFromLocalDb(mRealm);

        initUI();
    }

    private void initUI() {
        androidx.appcompat.widget.Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Locations DB");

        btnExportDb = findViewById(R.id.btnExportDb);
        btnExportDb.setOnClickListener(this);

        btnClearDb = findViewById(R.id.btnClearDb);
        btnClearDb.setOnClickListener(this);
        if (locations.size() > 0) {
            btnClearDb.setEnabled(true);
        } else {
            btnClearDb.setEnabled(false);
        }

        // recyclerview settings
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        // layout setting
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // get intent data
        /*
        final Object objectLocations = ((ObjectWrapperForBinder) getIntent().getExtras().getBinder("LOCATIONS_ARRAY_LIST")).getData();
        ArrayList<Location> locations = (ArrayList<Location>) objectLocations;
        Log.d("TAG", "onCreate: locations size: " + locations.size());
         */

        // adapter setting
        mAdapter = new LocationAdapter(locations);
        recyclerView.setAdapter(mAdapter);
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
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnClearDb) {
            ClearLocalDbDialog dialog = new ClearLocalDbDialog(this);
            dialog.show(getSupportFragmentManager(), "fr.vocality.gpstracker.dialogs.ClearLocalDbDialog");
        }
    }

    public void clearLocalDb() {
        DbUtils.clearLocalDb(mRealm);
        locations.clear();
        mAdapter.notifyDataSetChanged();
        btnClearDb.setEnabled(false);
    }
}