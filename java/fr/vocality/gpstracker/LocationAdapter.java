package fr.vocality.gpstracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.vocality.gpstracker.beans.Location;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
    private ArrayList<Location> mDataset;

    public LocationAdapter(ArrayList<Location> locations) {
        this.mDataset = locations;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView locationTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = (TextView) itemView.findViewById(R.id.lblItem);
        }
    }

    @NonNull
    @Override
    public LocationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new View
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View locationView = layoutInflater.inflate(R.layout.list_item, parent, false);
        ViewHolder vh = new ViewHolder(locationView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull LocationAdapter.ViewHolder holder, int position) {
        Location location = mDataset.get(position);
        TextView textView = holder.locationTextView;
        textView.setText(location.toString());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
