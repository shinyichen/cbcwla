package org.cbcwpa.android.cbcwlaapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.cbcwpa.android.cbcwlaapp.adapters.SermonsAdapter;
import org.cbcwpa.android.cbcwlaapp.services.MediaPlayerService;
import org.cbcwpa.android.cbcwlaapp.xml.SermonRSSParser;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.util.ArrayList;

public class SermonActivity extends AppCompatActivity implements MediaPlayerService.MediaListener{

    private static final String TAG = "SermonActivity";

    private static final String sermon_rss_path = "http://cbcwla.org/home/service-type/sunday-sermons/feed/";

    private ArrayList<Sermon> sermons = new ArrayList<>();

    RecyclerView sermonsListView;

    SermonsAdapter sermonsAdapter;

    private MediaPlayerService mediaPlayer;

    private boolean serviceBound = false;

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
            sermons = new SermonRSSParser().execute(sermon_rss_path).get();
        } catch (Exception e) {
            Log.e(TAG, "Error reading RSS feed");
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // main list view
        sermonsListView = (RecyclerView) findViewById(R.id.sermon_activity_list);
        sermonsAdapter = new SermonsAdapter(this, sermons, clickListener);
        sermonsListView.setAdapter(sermonsAdapter);
        sermonsListView.setLayoutManager(new LinearLayoutManager(this));

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            mediaPlayer.stopSelf();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sermon, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }

    // bind to MediaServicePlayer
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) iBinder;
            mediaPlayer = binder.getService();
            serviceBound = true;
            mediaPlayer.registerClient(SermonActivity.this);

//            Toast.makeText(SermonActivity.this, "Service bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private void playAudio(Sermon sermon) {
        String path = sermon.getAudioPath();
        String title = sermon.getTitle();
        String author = sermon.getAuthor();
        String date = sermon.getPubDate();
        if (!serviceBound) { // first time
            Intent intent = new Intent(this, MediaPlayerService.class);
            intent.putExtra("media", path);
            intent.putExtra("title", title);
            intent.putExtra("date", date);
            intent.putExtra("author", author);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // TODO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            broadcastIntent.putExtra("media", path);
            broadcastIntent.putExtra("title", title);
            broadcastIntent.putExtra("date", date);
            broadcastIntent.putExtra("author", author);
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

    private SermonsAdapter.ViewHolder currentSermonHolder;

    SermonsAdapter.sermonViewListener clickListener = new SermonsAdapter.sermonViewListener() {
        @Override
        public void onItemClicked(Sermon sermon, SermonsAdapter.ViewHolder holder) {
            if (holder.isPlaying()) {// pause pressed
                pauseAudio();
                holder.setPlayStatus(SermonsAdapter.PlaybackStatus.PAUSED);
            }
            else { // play pressed

                if (currentSermonHolder == null) {
                    // no existing audio, start new sermon
                    playAudio(sermon);
                    holder.setPlayStatus(SermonsAdapter.PlaybackStatus.PLAYING);
                    currentSermonHolder = holder;
                }
                else if (currentSermonHolder != holder) { // play pressed with existing audio
                    // pause existing audio
                    currentSermonHolder.setPlayStatus(SermonsAdapter.PlaybackStatus.STOPPED);
                    playAudio(sermon);
                    holder.setPlayStatus(SermonsAdapter.PlaybackStatus.PLAYING);
                    currentSermonHolder = holder;
                } else { // resume current sermon
                    resumeAudio();
                    holder.setPlayStatus(SermonsAdapter.PlaybackStatus.PLAYING);
                }

            }

        }
    };

    @Override
    public void paused() {
        currentSermonHolder.setPlayStatus(SermonsAdapter.PlaybackStatus.PAUSED);
    }

    @Override
    public void playing() {
        currentSermonHolder.setPlayStatus(SermonsAdapter.PlaybackStatus.PLAYING);
    }
}
