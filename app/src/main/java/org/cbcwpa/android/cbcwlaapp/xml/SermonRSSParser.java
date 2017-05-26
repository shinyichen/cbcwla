package org.cbcwpa.android.cbcwlaapp.xml;

import android.os.AsyncTask;

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


public class SermonRSSParser extends AsyncTask<String, Integer, ArrayList<Sermon>> {

    @Override
    protected ArrayList<Sermon> doInBackground(String... args) {

        String url = args[0];

        ArrayList<Sermon> sermons = new ArrayList<Sermon>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new URL(url).openStream());

            NodeList items = document.getDocumentElement().getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                Sermon sermon = new Sermon();
                sermon.setId(i);
                NodeList children = item.getChildNodes();
                for (int c = 0; c < children.getLength(); c++) {
                    Node child = children.item(c);
                    switch (child.getNodeName()) {
                        case "title":
                            sermon.setTitle(child.getLastChild().getTextContent().trim());
                            break;
                        case "link":
                            sermon.setLink(child.getLastChild().getTextContent().trim());
                            break;
                        case "pubDate":
                            String str = child.getLastChild().getTextContent().trim();
                            DateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                            DateFormat outputFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH);
                            Date date = inputFormat.parse(str);
                            String out = outputFormat.format(date);
                            sermon.setPubDate(out);
                            break;
                        case "itunes:author":
                            sermon.setAuthor(child.getLastChild().getTextContent().trim());
                            break;
                        case "enclosure":
                            String audioPath = child.getAttributes().getNamedItem("url").getNodeValue();
                            sermon.setAudioPath(audioPath);
                            break;
                    }
                }
                sermons.add(sermon);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sermons;
    }
}
