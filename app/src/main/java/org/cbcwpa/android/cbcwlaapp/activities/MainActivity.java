package org.cbcwpa.android.cbcwlaapp.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.services.MediaPlayerService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ImageView sermon_button, song_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sermon_button = (ImageView) findViewById(R.id.nav_sermon);
        song_button = (ImageView) findViewById(R.id.nav_song);

        sermon_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SermonActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });

        Log.i(TAG, "Created");

    }

    @Override
    protected void onStart() {
        super.onStart();

        // start media player service
        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {

        // stop media player service
        Intent intent = new Intent(this, MediaPlayerService.class);
        stopService(intent);

        super.onDestroy();

        Log.i(TAG, "Destroyed");
    }
}
