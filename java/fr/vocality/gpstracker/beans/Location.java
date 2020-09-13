package fr.vocality.gpstracker.beans;

import java.io.Serializable;
import java.util.ArrayList;

import io.realm.RealmObject;

public class Location extends RealmObject {
    private double mLatitude;
    private double mLongitude;
    private String mTimestamp;
    private String trackId;

    @Override
    public String toString() {
        return "Location{" +
                "mLatitude=" + mLatitude +
                ", mLongitude=" + mLongitude +
                ", mTimestamp='" + mTimestamp + '\'' +
                ", trackId='" + trackId + '\'' +
                '}';
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public void setTimestamp(String mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

}
