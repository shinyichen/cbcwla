package org.cbcwpa.android.cbcwlaapp.xml;


import android.os.AsyncTask;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SongRSSParser extends AsyncTask<String, Integer, ArrayList<Song>> {

    @Override
    protected ArrayList<Song> doInBackground(String... args) {

        String url = args[0];

        ArrayList<Song> songs = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new URL(url).openStream());

            NodeList items = document.getDocumentElement().getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                Song song = new Song();
                NodeList children = item.getChildNodes();
                for (int c = 0; c < children.getLength(); c++) {
                    Node child = children.item(c);
                    switch (child.getNodeName()) {
                        case "title":
                            song.setTitle(child.getLastChild().getTextContent().trim());
                            break;
                        case "link":
                            song.setLink(child.getLastChild().getTextContent().trim());
                            song.setId(song.getLink()); // use link as ID
                            break;
                        case "pubDate":
                            String str = child.getLastChild().getTextContent().trim();
                            DateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                            DateFormat outputFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH);
                            Date date = inputFormat.parse(str);
                            String out = outputFormat.format(date);
                            song.setPubDate(out);
                            break;
                        case "description":
                            song.setDescription(((CharacterData) child.getFirstChild()).getData());
                            break;
                        case "content:encoded":
                            song.setContent(((CharacterData) child.getFirstChild()).getData());
                            break;
                    }
                }
                songs.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return songs;
    }
}
