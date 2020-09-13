package fr.vocality.gpstracker;

import android.os.Binder;

public class ObjectWrapperForBinder extends Binder {
    private final Object mData;

    public ObjectWrapperForBinder(Object data) {
        this.mData = data;
    }

    public Object getData() {
        return mData;
    }
}
