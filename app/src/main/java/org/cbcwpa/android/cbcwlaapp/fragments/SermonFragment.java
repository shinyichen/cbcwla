package org.cbcwpa.android.cbcwlaapp.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.adapters.SermonsAdapter;
import org.cbcwpa.android.cbcwlaapp.services.Constants;
import org.cbcwpa.android.cbcwlaapp.services.MediaPlayerService;
import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SermonFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SermonFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SermonFragment extends Fragment implements MediaPlayerService.MediaListener {

    private static final String TAG = "SermonFragment";

    private ArrayList<Sermon> sermons = new ArrayList<>();

    RecyclerView sermonsListView;

    SermonsAdapter sermonsAdapter;

    private MediaPlayerService mediaPlayerService;

    private boolean serviceBound = false;

    private int currentSermonId = -1;

    private Sermon playOnServiceConnect;

    public SermonFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SermonFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SermonFragment newInstance(ArrayList<Sermon> sermons) {
        SermonFragment fragment = new SermonFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("sermons", sermons);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sermons = getArguments().getParcelableArrayList("sermons");
        }
        try {
            if (savedInstanceState != null) {
                currentSermonId = savedInstanceState.getInt("CurrentSermonId", -1);
            } else {
                currentSermonId = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading RSS feed");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_sermon, container, false);

        // main list view
        sermonsListView = (RecyclerView) rootView.findViewById(R.id.sermon_activity_list);
        sermonsAdapter = new SermonsAdapter(sermons, clickListener);
        sermonsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        sermonsListView.setAdapter(sermonsAdapter);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // bind to media player service if it's running (audio is playing)
        // if it's not running, start the service only when audio play is requested
        if (MediaPlayerService.isRunning) {
            Intent intent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      savedInstanceState.putInt("CurrentSermonId", currentSermonId);
//        savedInstanceState.putParcelableArrayList("Sermons", sermons);
        super.onSaveInstanceState(savedInstanceState);
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

        // unbind from media player service
        if (serviceBound) {
            mediaPlayerService.unregisterClient();
            getActivity().unbindService(serviceConnection);
        }

        Log.i(TAG, "destroyed");
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    // bind to MediaServicePlayer
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            mediaPlayerService = binder.getService();
            serviceBound = true;
            mediaPlayerService.registerClient(SermonFragment.this);

            // if service already existed, get current play status
            int id = mediaPlayerService.getCurrentSermonId();
            for (Sermon s : sermons) {
                if (s.getId() == id) {
                    s.setStatus(mediaPlayerService.getStatus());
                }
            }
            currentSermonId = id;

            if (playOnServiceConnect != null) {
                Intent broadcastIntent = new Intent(Constants.Broadcast_PLAY_NEW_AUDIO);
                broadcastIntent.putExtra("sermon", playOnServiceConnect);
                getActivity().sendBroadcast(broadcastIntent);
                playOnServiceConnect = null;
            }

//            Toast.makeText(SermonActivity.this, "Service bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mediaPlayerService.unregisterClient();
            serviceBound = false;
        }
    };
    private void playAudio(Sermon sermon) {

        // we didn't make connection when activity started because service was not active
        // (there was no audio playing)
        // so now we need to start the service
        if (!serviceBound) {
            playOnServiceConnect = sermon;
            Intent playerIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(playerIntent);
            getActivity().bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            Intent broadcastIntent = new Intent(Constants.Broadcast_PLAY_NEW_AUDIO);
            broadcastIntent.putExtra("sermon", sermon);
            getActivity().sendBroadcast(broadcastIntent);
        }
    }

    private void pauseAudio() {
        Intent broadcastIntent = new Intent(Constants.Broadcast_PAUSE_AUDIO);
        getActivity().sendBroadcast(broadcastIntent);
    }

    private void resumeAudio() {
        Intent broadcastIntent = new Intent(Constants.Broadcast_RESUME_AUDIO);
        getActivity().sendBroadcast(broadcastIntent);
    }

    SermonsAdapter.sermonViewListener clickListener = new SermonsAdapter.sermonViewListener() {
        @Override
        public void onItemClicked(Sermon sermon, SermonsAdapter.ViewHolder holder) {

            if (currentSermonId != -1 && sermon.getId() == currentSermonId) {
                // clicked on the current sermon, flip play status
                if (mediaPlayerService.getStatus() == PlaybackStatus.PLAYING)
                    pauseAudio();
                else
                    resumeAudio();
            } else {
                // play new audio
                playAudio(sermon);
            }


        }
    };

    @Override
    public void paused() {
        if (currentSermonId != -1) {
            sermons.get(currentSermonId).setStatus(PlaybackStatus.PAUSED);
            sermonsAdapter.notifyItemChanged(currentSermonId);
        }
    }

    @Override
    public void playing(int sermonId) {
        if (currentSermonId == -1) {
            sermons.get(sermonId).setStatus(PlaybackStatus.PLAYING);
            sermonsAdapter.notifyItemChanged(sermonId);
            currentSermonId = sermonId;
        } else if (currentSermonId != sermonId) { // stop current, play new
            sermons.get(currentSermonId).setStatus(PlaybackStatus.STOPPED);
            sermonsAdapter.notifyItemChanged(currentSermonId);
            sermons.get(sermonId).setStatus(PlaybackStatus.PLAYING);
            sermonsAdapter.notifyItemChanged(sermonId);
            currentSermonId = sermonId;
        } else {
            // resume current sermon
            sermons.get(currentSermonId).setStatus(PlaybackStatus.PLAYING);
            sermonsAdapter.notifyItemChanged(sermonId);
        }
    }

    @Override
    public void stopped() {
        if (currentSermonId != -1) {
            sermons.get(currentSermonId).setStatus(PlaybackStatus.STOPPED);
            sermonsAdapter.notifyItemChanged(currentSermonId);
            currentSermonId = -1;
        }
    }
}
