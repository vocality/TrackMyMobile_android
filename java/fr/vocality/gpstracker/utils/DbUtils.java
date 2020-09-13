package fr.vocality.gpstracker.utils;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

import fr.vocality.gpstracker.beans.Location;
import fr.vocality.gpstracker.beans.Track;
import io.realm.Realm;
import io.realm.RealmResults;

public class DbUtils {
    public static ArrayList<Location> readLocationsFromLocalDb(Realm defaultInstanceDb) {
        final ArrayList<Location> locations = new ArrayList<Location>();

        try {
            defaultInstanceDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(@NotNull Realm realm) {
                    RealmResults<Location> queryLocations = realm.where(Location.class).findAll();
                    //Log.d(TAG, String.format("[execute()] timestamp: %s - Lat: %s - Long: %s", loc.getTimestamp(), loc.getLatitude(), loc.getLongitude()));
                    locations.addAll(queryLocations);
                }
            });
        } finally {
            if (defaultInstanceDb != null) {
                //mRealm.close();
            }
        }
        return locations;
    }

    public static void clearLocalDb(Realm defaultInstanceDb) {
        try  {
            defaultInstanceDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                }
            });

        } finally {
            if (defaultInstanceDb != null) {
                //mRealm.close();
            }
        }
    }

    public static void saveLocationToLocalDb(final double latitude, final double longitude, final String timestamp, final String trackId, final Realm defaultInstanceDb) {
        try {
            defaultInstanceDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    //Log.d(TAG, "execute: save location object in realm");
                    Location location = defaultInstanceDb.createObject(Location.class);
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    location.setTimestamp(timestamp);
                    location.setTrackId(trackId);
                    realm.insert(location);

                    // read datas from db

                }
            });
        } finally {
            if (defaultInstanceDb != null) {
                //mRealm.close();
            }
        }
    }

    // TODO
    public static void dumpLocalDb(Realm defaultInstanceDb, String url) {

    }
}
