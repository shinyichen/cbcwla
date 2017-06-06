package org.cbcwpa.android.cbcwlaapp.xml;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    private int id;
    private String title;
    private String link;
    private String pubDate;
    private String description;
    private String content;

    public Song() {

    }

    private Song(Parcel p) {
        this.id = p.readInt();
        this.title = p.readString();
        this.link = p.readString();
        this.pubDate = p.readString();
        this.description = p.readString();
        this.content = p.readString();
    }
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

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(link);
        dest.writeString(pubDate);
        dest.writeString(description);
        dest.writeString(content);
    }

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
