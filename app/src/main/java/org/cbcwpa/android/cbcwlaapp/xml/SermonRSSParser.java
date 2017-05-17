package org.cbcwpa.android.cbcwlaapp.xml;

import android.os.AsyncTask;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.ArrayList;

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
                            sermon.setPubDate(child.getLastChild().getTextContent().trim());
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
