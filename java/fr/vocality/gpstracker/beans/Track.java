package fr.vocality.gpstracker.beans;

import java.io.Serializable;

public class Track implements Serializable {
    private String mId;
    private String mName;
    private String mStartTime;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public void setStartTime(String mStartTime) {
        this.mStartTime = mStartTime;
    }

    @Override
    public String toString() {
        return "Track{" +
                "mId='" + mId + '\'' +
                ", mName='" + mName + '\'' +
                ", mStartTime='" + mStartTime + '\'' +
                '}';
    }
}
