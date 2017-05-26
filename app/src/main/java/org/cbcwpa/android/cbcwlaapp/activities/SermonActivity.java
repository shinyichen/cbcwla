package org.cbcwpa.android.cbcwlaapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.adapters.SermonsAdapter;
import org.cbcwpa.android.cbcwlaapp.services.MediaPlayerService;
import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;
import org.cbcwpa.android.cbcwlaapp.xml.SermonRSSParser;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.util.ArrayList;

public class SermonActivity extends AppCompatActivity implements MediaPlayerService.MediaListener{

    private static final String TAG = "SermonActivity";

    private static final String sermon_rss_path = "http://cbcwla.org/home/service-type/sunday-sermons/feed/";

    private ArrayList<Sermon> sermons = new ArrayList<>();

    RecyclerView sermonsListView;

    SermonsAdapter sermonsAdapter;

    private MediaPlayerService mediaPlayerService;

    private boolean serviceBound = false;

    private int currentSermonId = -1;

    private Sermon playOnServiceConnect;

    public static final String Broadcast_PLAY_NEW_AUDIO = "org.cbcwla.android.PlayNewAudio";

    public static final String Broadcast_PAUSE_AUDIO = "org.cbcwla.android.PauseAudio";

    public static final String Broadcast_RESUME_AUDIO = "org.cbcwla.android.ResumeAudio";

    public SermonActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sermon);

        try {
            if (savedInstanceState != null) {
                currentSermonId = savedInstanceState.getInt("CurrentSermonId", -1);
                sermons = savedInstanceState.getParcelableArrayList("Sermons");
            } else {
                sermons = new SermonRSSParser().execute(sermon_rss_path).get();
                currentSermonId = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading RSS feed");
        }

        try {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        // main list view
        sermonsListView = (RecyclerView) findViewById(R.id.sermon_activity_list);
        sermonsAdapter = new SermonsAdapter(this, sermons, clickListener);
        sermonsListView.setLayoutManager(new LinearLayoutManager(this));
        sermonsListView.setAdapter(sermonsAdapter);

        Log.i(TAG, "SermonActivity created");

    }

    @Override
    protected void onStart() {
        super.onStart();

        // bind to media player service if it's running (audio is playing)
        // if it's not running, start the service only when audio play is requested
        if (MediaPlayerService.isRunning) {
            Intent intent = new Intent(this, MediaPlayerService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("CurrentSermonId", currentSermonId);
        savedInstanceState.putParcelableArrayList("Sermons", sermons);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        // up goes to parent (MainActivity)
        // back by default goes to previous activity on stack
//        onSupportNavigateUp();
        navigateUp();
    }


    @Override
    public boolean supportShouldUpRecreateTask(@NonNull Intent targetIntent) {
        // if user click on notification and parent activity is gone
        // this will recreate parent activity when navigate up
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigateUp();
        return true;
    }

    private void navigateUp() {
        // go up to the parent activity without recreating the parent
        Intent h = NavUtils.getParentActivityIntent(this);
        h.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, h);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unbind from media player service
        if (serviceBound) {
            mediaPlayerService.unregisterClient();
            unbindService(serviceConnection);
        }

        Log.i(TAG, "destroyed");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_sermon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == android.R.id.home) { // up button
//            NavUtils.navigateUpFromSameTask(this);
            navigateUp();
        }

        return super.onOptionsItemSelected(item);
    }

    // bind to MediaServicePlayer
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            mediaPlayerService = binder.getService();
            serviceBound = true;
            mediaPlayerService.registerClient(SermonActivity.this);

            // if service already existed, get current play status
            int id = mediaPlayerService.getCurrentSermonId();
            for (Sermon s : sermons) {
                if (s.getId() == id) {
                    s.setStatus(mediaPlayerService.getStatus());
                }
            }
            currentSermonId = id;

            if (playOnServiceConnect != null) {
                Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                broadcastIntent.putExtra("sermon", playOnServiceConnect);
                sendBroadcast(broadcastIntent);
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
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            broadcastIntent.putExtra("sermon", sermon);
            sendBroadcast(broadcastIntent);
        }
    }

    private void pauseAudio() {
        Intent broadcastIntent = new Intent(Broadcast_PAUSE_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    private void resumeAudio() {
        Intent broadcastIntent = new Intent(Broadcast_RESUME_AUDIO);
        sendBroadcast(broadcastIntent);
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
