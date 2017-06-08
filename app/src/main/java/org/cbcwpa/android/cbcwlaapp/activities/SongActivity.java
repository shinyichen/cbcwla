package org.cbcwpa.android.cbcwlaapp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.xml.Song;

public class SongActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        Song song = getIntent().getParcelableExtra("song");

        Toolbar toolbar = (Toolbar) findViewById(R.id.song_toolbar);
        toolbar.setTitle(song.getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView webView = (WebView) findViewById(R.id.song_web_view);
        webView.loadData(song.getContent(), "text/html; charset=UTF-8", null);
    }
}
