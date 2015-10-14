package com.paulpjryan.bpmplayer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private Song[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView titleView;
        TextView artistView;
        public ViewHolder(View v) {
            super(v);

            titleView = (TextView)v.findViewById(R.id.song_title);
            artistView = (TextView)v.findViewById(R.id.song_artist);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SongAdapter(Song[] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.titleView.setText(mDataset[position].getTitle());
        holder.artistView.setText(mDataset[position].getArtist());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}