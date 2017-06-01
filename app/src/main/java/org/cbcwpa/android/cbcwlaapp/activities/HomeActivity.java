package org.cbcwpa.android.cbcwlaapp.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.fragments.SermonFragment;
import org.cbcwpa.android.cbcwlaapp.fragments.SongFragment;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;
import org.cbcwpa.android.cbcwlaapp.xml.SermonRSSParser;
import org.cbcwpa.android.cbcwlaapp.xml.Song;
import org.cbcwpa.android.cbcwlaapp.xml.SongRSSParser;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    public static final String TAG = "HomeActivity";

    private ArrayList<Sermon> sermons;

    private ArrayList<Song> songs;

    private static final String sermon_rss_path = "http://cbcwla.org/home/service-type/sunday-sermons/feed/";

    private static final String song_rss_path = "http://cbcwla.org/home/blog/category/resources/life-is-a-song/feed";


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()) {
                        case R.id.navigation_sermon:
                            if (sermons == null || sermons.isEmpty()) {
                                try {
                                    sermons = new SermonRSSParser().execute(sermon_rss_path).get();
                                } catch (Exception e) {
                                    sermons = new ArrayList<>();
                                }
                            }

                            selectedFragment = SermonFragment.newInstance(sermons);
                            break;
                        case R.id.navigation_song:
                            if (songs == null || songs.isEmpty()) {
                                try {
                                    songs = new SongRSSParser().execute(song_rss_path).get();
                                } catch (Exception e) {
                                    songs = new ArrayList<>();
                                }
                            }
                            selectedFragment = SongFragment.newInstance(songs);
                            break;
                    }
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content, selectedFragment);
                    transaction.commit();
                    return true;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        navigation.setSelectedItemId(R.id.navigation_sermon);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("Sermons", sermons);
        outState.putParcelableArrayList("Songs", songs);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        Log.i(TAG, "restore state");
        sermons = savedInstanceState.getParcelableArrayList("Sermons");
        songs = savedInstanceState.getParcelableArrayList("Songs");
    }

    @Override
    protected void onDestroy() {
        // save data
        super.onDestroy();
    }
}
