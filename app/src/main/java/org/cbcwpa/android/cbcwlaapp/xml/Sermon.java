package org.cbcwpa.android.cbcwlaapp.xml;

import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;

import java.io.Serializable;

public class Sermon implements Serializable{

    private int id;
    private String title;
    private String link;
    private String pubDate;
    private String description;
    private String author;
    private String audioPath;
    private PlaybackStatus status = PlaybackStatus.STOPPED;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void setStatus(PlaybackStatus status) {
        this.status = status;
    }

    public PlaybackStatus getStatus() {
        return status;
    }

}
