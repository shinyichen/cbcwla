package org.cbcwpa.android.cbcwlaapp.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.xml.Song;

import java.util.ArrayList;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

    public interface SongViewListener {
        void onItemClicked(Song song , SongsAdapter.ViewHolder holder);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_song_list, parent, false);
        return new ViewHolder(view);
    }

    private ArrayList<Song> songs;

    private final SongViewListener clickListener;

    public SongsAdapter(ArrayList<Song> songs, SongViewListener listener) {
        this.songs = songs;
        this.clickListener = listener;
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
        this.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Song song = songs.get(position);
        holder.songTitleView.setText(song.getTitle());
        holder.songDateView.setText(song.getPubDate());
        holder.songDescriptionView.setText(song.getDescription());

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != clickListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    clickListener.onItemClicked(song, holder);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        final TextView songTitleView;
        final TextView songDateView;
        final TextView songDescriptionView;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            songTitleView = (TextView) view.findViewById(R.id.song_title_view);
            songDateView = (TextView) view.findViewById(R.id.song_date_view);
            songDescriptionView = (TextView) view.findViewById(R.id.song_description_view);
        }

    }
}
