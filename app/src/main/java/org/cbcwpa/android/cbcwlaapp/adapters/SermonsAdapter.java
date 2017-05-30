package org.cbcwpa.android.cbcwlaapp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.util.ArrayList;


public class SermonsAdapter extends RecyclerView.Adapter<SermonsAdapter.ViewHolder> {

    public interface sermonViewListener {
        void onItemClicked(Sermon sermon ,ViewHolder holder);
    }


    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder{

        View container;
        ImageButton playButton;
        TextView titleView;
        TextView authorView;
        TextView dateView;
        PlaybackStatus status = PlaybackStatus.STOPPED;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        private ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            container = itemView;
            playButton = (ImageButton) itemView.findViewById(R.id.sermon_play_button);
            titleView = (TextView) itemView.findViewById(R.id.sermon_title);
            authorView = (TextView) itemView.findViewById(R.id.sermon_author);
            dateView = (TextView) itemView.findViewById(R.id.sermon_date);
        }

        public void setPlayStatus(PlaybackStatus status) {
            this.status = status;
            if (status == PlaybackStatus.PLAYING) {
                playButton.setImageResource(R.drawable.ic_pause_circle_outline_black_48dp);
                container.setSelected(true);
            } else if (status == PlaybackStatus.PAUSED) {
                playButton.setImageResource(R.drawable.ic_play_circle_outline_black_48dp);
                container.setSelected(true);
            } else if (status == PlaybackStatus.STOPPED) {
                playButton.setImageResource(R.drawable.ic_play_circle_outline_black_48dp);
                container.setSelected(false);
            }
        }

        public boolean isPlaying() {
            return status == PlaybackStatus.PLAYING;
        }

    }

    private ArrayList<Sermon> sermons;

    private final sermonViewListener clickListener;

    public SermonsAdapter(ArrayList<Sermon> sermons, sermonViewListener listener) {
        this.sermons = sermons;
        this.clickListener = listener;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View sermonView = inflater.inflate(R.layout.sermon_list_item, parent, false);

        return new ViewHolder(sermonView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Sermon sermon = sermons.get(position);

        View container = holder.container;
        TextView titleView = holder.titleView;
        titleView.setText(sermon.getTitle());
        TextView authorView = holder.authorView;
        authorView.setText(sermon.getAuthor());
        TextView dateView = holder.dateView;
        dateView.setText(sermon.getPubDate());

        ImageButton playButton = holder.playButton;
        playButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                clickListener.onItemClicked(sermon, holder);
            }
        });

        PlaybackStatus status = sermon.getStatus();
        if (status == PlaybackStatus.PLAYING) {
            playButton.setImageResource(R.drawable.ic_pause_circle_outline_black_48dp);
            container.setSelected(true);
        } else if (status == PlaybackStatus.PAUSED) {
            playButton.setImageResource(R.drawable.ic_play_circle_outline_black_48dp);
            container.setSelected(true);
        } else if (status == PlaybackStatus.STOPPED) {
            playButton.setImageResource(R.drawable.ic_play_circle_outline_black_48dp);
            container.setSelected(false);
        }
    }



    @Override
    public int getItemCount() {
        return sermons.size();
    }

}
