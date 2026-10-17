package ru.opds.client;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class OpdsParser {

    public static class Format {
        public String link;
        public String type;
        public String fileName;
        public Format(String link, String type) {
            this.link = link;
            this.type = type;
            this.fileName = strippedFileName(link);
        }
    }

    public static class Entry {
        public String title;
        public String link;      // acquisition: acquisition URL or child feed URL
        public String type;      // MIME type of extraction link, or "feed"
        public String summary;
        public String author;
        public String description;
        public String coverUrl;
        public List<Format> formats = new ArrayList<Format>(); // available download formats

        public boolean isFeed;
        public boolean isBook;
        public String fileName;
    }

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final String OPDS_NS = "http://opds-spec.org/2010/catalog";
    private static final String DLL_NS = "http://www.daisy.org/ns/pipeline/";
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    public static class FeedInfo {
        public List<Entry> entries;
        public String searchTemplate; // OPDS OpenSearch template, may contain {searchTerms}
    }

    public FeedInfo parseFeed(InputStream in) {
        FeedInfo info = new FeedInfo();
        List<Entry> entries = new ArrayList<Entry>();
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            try {
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception fe) {
                android.util.Log.e("OPDSClient", "disallow-doctype not supported", fe);
            }

            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(in);
            info.searchTemplate = findSearchTemplate(doc.getDocumentElement());
            NodeList list = doc.getElementsByTagNameNS(ATOM_NS, "entry");
            for (int i = 0; i < list.getLength(); i++) {
                Entry e = parseEntry((Element) list.item(i));
                if (e != null) {
                    entries.add(e);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("OPDSClient", "parse failed", e);
            return null;
        }
        info.entries = entries;
        return info;
    }

    public List<Entry> parse(InputStream in) {
        FeedInfo info = parseFeed(in);
        return info == null ? null : info.entries;
    }

    private static String findSearchTemplate(Element root) {
        NodeList links = root.getElementsByTagNameNS(ATOM_NS, "link");
        String fallback = null;
        for (int i = 0; i < links.getLength(); i++) {
            Element l = (Element) links.item(i);
            String rel = l.getAttribute("rel");
            String href = l.getAttribute("href");
            String type = l.getAttribute("type");
            if (rel != null && "search".equals(rel) && href != null && href.length() > 0) {
                // Prefer the template with {searchTerms}
                if (href.contains("{searchTerms}")) {
                    return href;
                }
                if (fallback == null) {
                    fallback = href;
                }
            }
        }
        return fallback;
    }

    private Entry parseEntry(Element entry) {
        Entry e = new Entry();
        e.title = childText(entry, "title");
        e.summary = childText(entry, "summary");
        e.author = childText(entry, "author/name");
        if (e.author == null || e.author.length() == 0) {
            e.author = childText(entry, "author");
        }
        e.description = childText(entry, "content");

        // Find link with rel=acquisition or navigation. Prefer book acquisition.
        NodeList links = entry.getElementsByTagNameNS(ATOM_NS, "link");
        String navHref = null;
        e.coverUrl = findLinkHref(links, new String[]{
                "http://opds-spec.org/image/thumbnail",
                "http://opds-spec.org/image", "image", "thumbnail"});
        android.util.Log.e("OPDSClient", "entry title='"+e.title+"' cover="+e.coverUrl+" author="+e.author);
        for (int i = 0; i < links.getLength(); i++) {
            Element l = (Element) links.item(i);
            String rel = l.getAttribute("rel");
            String href = l.getAttribute("href");
            String type = l.getAttribute("type");
            if (href == null || href.length() == 0) {
                continue;
            }
            if ("subsection".equals(rel) || "related".equals(rel)) {
                if (navHref == null) navHref = href;
                continue;
            }
            if (rel == null || rel.length() == 0 || "acquisition".equals(rel) || "http://opds-spec.org/acquisition".equals(rel)) {
                if ("feed".equals(type)) {
                    // Direct acquisition with feed type = child catalog embedded
                    if (navHref == null) navHref = href;
                } else {
                    // A downloadable book file (possibly several formats)
                    Format f = new Format(href, type);
                    e.formats.add(f);
                    if (e.link == null) {
                        e.link = href;
                        e.type = type;
                        e.isBook = true;
                        e.fileName = f.fileName;
                    }
                }
            }
        }

        if (e.link == null && navHref != null) {
            e.link = navHref;
            e.isFeed = true;
        } else if (e.link == null) {
            return null; // no usable link
        }
        return e;
    }

    private static String childText(Element parent, String path) {
        Node node = parent;
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (node == null || !(node instanceof Element)) {
                return "";
            }
            NodeList nl = ((Element) node).getElementsByTagNameNS(ATOM_NS, parts[i]);
            if (nl.getLength() == 0) {
                return "";
            }
            node = nl.item(0);
        }
        return node == null ? "" : (node.getTextContent() == null ? "" : node.getTextContent().trim());
    }

    private static String findLinkHref(NodeList links, String[] rels) {
        for (int i = 0; i < links.getLength(); i++) {
            Element l = (Element) links.item(i);
            String rel = l.getAttribute("rel");
            String href = l.getAttribute("href");
            if (href == null || href.length() == 0) {
                continue;
            }
            for (String r : rels) {
                if (r.equals(rel)) {
                    return href;
                }
            }
        }
        return null;
    }

    private static String strippedFileName(String url) {
        // Remove query
        int q = url.indexOf('?');
        if (q >= 0) url = url.substring(0, q);
        int slash = url.lastIndexOf('/');
        String name = slash >= 0 ? url.substring(slash + 1) : url;
        if (name.length() == 0) {
            return "book";
        }
        // strip HTML from name
        return name.replaceAll("[^A-Za-z0-9._\\-]", "_");
    }
}