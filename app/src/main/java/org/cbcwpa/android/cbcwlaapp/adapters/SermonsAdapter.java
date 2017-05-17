package org.cbcwpa.android.cbcwlaapp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.util.ArrayList;


public class SermonsAdapter extends RecyclerView.Adapter<SermonsAdapter.ViewHolder> {

    public interface sermonViewListener {
        void onItemClicked(Sermon sermon);
    }


    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    static class ViewHolder extends RecyclerView.ViewHolder{

        ImageButton playButton;
        TextView titleView;
        TextView authorView;
        TextView dateView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        private ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            playButton = (ImageButton) itemView.findViewById(R.id.sermon_play_button);
            titleView = (TextView) itemView.findViewById(R.id.sermon_title);
            authorView = (TextView) itemView.findViewById(R.id.sermon_author);
            dateView = (TextView) itemView.findViewById(R.id.sermon_date);
        }

    }

    private ArrayList<Sermon> sermons;

    private Context context;

    private final sermonViewListener clickListener;

    public SermonsAdapter(Context context, ArrayList<Sermon> sermons, sermonViewListener listener) {
        this.context = context;
        this.sermons = sermons;
        this.clickListener = listener;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View sermonView = inflater.inflate(R.layout.sermon_list_item, parent, false);

        return new ViewHolder(sermonView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Sermon sermon = sermons.get(position);

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
                clickListener.onItemClicked(sermon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sermons.size();
    }
}
