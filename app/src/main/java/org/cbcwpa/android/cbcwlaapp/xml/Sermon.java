package org.cbcwpa.android.cbcwlaapp.xml;

import android.os.Parcel;
import android.os.Parcelable;

import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;

public class Sermon implements Parcelable{

    private String id;
    private String title;
    private String link;
    private String pubDate;
    private String description;
    private String author;
    private String audioPath;
    private PlaybackStatus status = PlaybackStatus.STOPPED;

    Sermon() {

    }

    private Sermon(Parcel p) {
        this.id = p.readString();
        this.title = p.readString();
        this.link = p.readString();
        this.pubDate = p.readString();
        this.description = p.readString();
        this.author = p.readString();
        this.audioPath = p.readString();
        this.status = PlaybackStatus.valueOf(p.readString());
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(link);
        dest.writeString(pubDate);
        dest.writeString(description);
        dest.writeString(author);
        dest.writeString(audioPath);
        dest.writeString(status.name());
    }

    public static final Parcelable.Creator<Sermon> CREATOR = new Parcelable.Creator<Sermon>() {
        public Sermon createFromParcel(Parcel in) {
            return new Sermon(in);
        }

        public Sermon[] newArray(int size) {
            return new Sermon[size];
        }
    };

}
