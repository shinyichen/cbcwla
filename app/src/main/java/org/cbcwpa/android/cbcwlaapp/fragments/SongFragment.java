package org.cbcwpa.android.cbcwlaapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.activities.SongActivity;
import org.cbcwpa.android.cbcwlaapp.adapters.SongsAdapter;
import org.cbcwpa.android.cbcwlaapp.xml.Song;

import java.util.ArrayList;

public class SongFragment extends Fragment {

    public static final String TAG = "SongFragment";

    private ArrayList<Song> songs = new ArrayList<>();

    private RecyclerView songsListView;

    private SongsAdapter songsAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SongFragment() {
    }

    public static SongFragment newInstance(ArrayList<Song> songs) {
        SongFragment fragment = new SongFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("songs", songs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            songs = getArguments().getParcelableArrayList("songs");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song, container, false);

        songsListView = (RecyclerView) view.findViewById(R.id.song_list_view);
        songsAdapter = new SongsAdapter(songs, clickListener);
        songsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        songsListView.setAdapter(songsAdapter);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "destroyed");
    }

    SongsAdapter.SongViewListener clickListener = new SongsAdapter.SongViewListener() {
        @Override
        public void onItemClicked(Song song, SongsAdapter.ViewHolder holder) {

            Intent i = new Intent(getActivity(), SongActivity.class);
            i.putExtra("song", song);
            startActivity(i);

        }
    };



}
