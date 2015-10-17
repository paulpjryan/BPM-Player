package com.paulpjryan.bpmplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    //private Song[] mDataset;
    private ArrayList<Song> mDataset;
    private ArrayList<Song> mOriginalData;
    private int selectedItem = -1;
    private Context mContext;

    private int lowLim = -2;
    private int highLim = 999;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView titleView;
        TextView artistView;
        TextView bpmView;
        public ViewHolder(View v) {
            super(v);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    notifyItemChanged(selectedItem);
                    selectedItem = getLayoutPosition();
                    notifyItemChanged(selectedItem);
                    if (mContext instanceof MainActivity) {
                        ((MainActivity) mContext).songPicked(view);
                    }
                }
            });

            titleView = (TextView)v.findViewById(R.id.song_title);
            artistView = (TextView)v.findViewById(R.id.song_artist);
            bpmView = (TextView)v.findViewById(R.id.song_bpm);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SongAdapter(ArrayList<Song> myDataset, Context context) {
        mDataset = myDataset;
        mOriginalData = new ArrayList<>(mDataset);
        mContext = context;
    }

    public void selectNext() {
        //Log.d("SelectAdapter", "Selecting next");
        notifyItemChanged(selectedItem);
        if(selectedItem < mDataset.size() - 1) {
            selectedItem++;
            //Log.d("SelectAdapter", "New Item: " + selectedItem);
        }
        else {
            selectedItem = 0;
            //Log.d("SelectAdapter", "New Item: " + selectedItem);
        }
        notifyItemChanged(selectedItem);
    }

    public void filterByBpm(int lowLim, int highLim) {
        mDataset.clear();
        Log.d("BPM Filter", "Checking DataSet");
        for(Song s : mOriginalData) {
            Log.d("BPM Filter", "Checking that BPM " + s.getBpm() + " is between " + lowLim + " and " + highLim);
            if(s.getBpm() >= lowLim && s.getBpm() <= highLim) {
                Log.d("BPM Filter", "Adding song");
                mDataset.add(s);
            }
        }
        notifyDataSetChanged();
    }

    public void setLowLim(int lowLim) {
        this.lowLim = lowLim;
        filterByBpm(lowLim, highLim);
    }

    public void setHighLim(int highLim) {
        this.highLim = highLim;
        filterByBpm(lowLim, highLim);
    }

    public void selectPrev() {
        //Log.d("SelectAdapter", "Selecting prev");
        notifyItemChanged(selectedItem);
        if(selectedItem > 0) {
            selectedItem--;
            //Log.d("SelectAdapter", "New Item: " + selectedItem);
        }
        else {
            selectedItem = mDataset.size() - 1;
            //Log.d("SelectAdapter", "New Item: " + selectedItem);
        }
        notifyItemChanged(selectedItem);
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
        holder.itemView.setSelected(selectedItem == position);

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        /* Array Dataset
        holder.titleView.setText(mDataset[position].getTitle());
        holder.artistView.setText(mDataset[position].getArtist());
        if(mDataset[position].getBpm() > 0) {
            holder.bpmView.setText("" + mDataset[position].getBpm());
        } else
            holder.bpmView.setText("");
        */


        holder.titleView.setText(mDataset.get(position).getTitle());
        holder.artistView.setText(mDataset.get(position).getArtist());
        if(mDataset.get(position).getBpm() > 0) {
            holder.bpmView.setText("" + mDataset.get(position).getBpm());
        } else
            holder.bpmView.setText("");
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
