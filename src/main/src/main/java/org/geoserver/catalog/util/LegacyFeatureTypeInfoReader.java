/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geoserver.platform.resource.Resource;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Element;

/**
 * Reads a legacy GeoServer 1.x feature type info.xml file.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyFeatureTypeInfoReader {

    /** Root featureType element. */
    Element featureType;

    /** The directory containing the feature type info.xml file */
    Resource parentDirectory;

    /**
     * Parses the info.xml file into a DOM.
     *
     * <p>This method *must* be called before any other methods.
     *
     * @param file The info.xml file.
     * @throws IOException In event of a parser error.
     */
    public void read(Resource file) throws IOException {
        parentDirectory = file.parent();
        Reader reader = XmlCharsetDetector.getCharsetAwareReader(file.in());

        try {
            featureType = ReaderUtils.parse(reader);
        } finally {
            reader.close();
        }
    }

    public String dataStore() throws Exception {
        return ReaderUtils.getAttribute(featureType, "datastore", true);
    }

    public String name() {
        return ReaderUtils.getChildText(featureType, "name");
    }

    public String alias() {
        return ReaderUtils.getChildText(featureType, "alias");
    }

    public String srs() throws Exception {
        return ReaderUtils.getChildText(featureType, "SRS");
    }

    public int srsHandling() {
        String s = ReaderUtils.getChildText(featureType, "SRSHandling");
        if (s == null || "".equals(s)) {
            return -1;
        }

        return Integer.parseInt(s);
    }

    public String title() {
        return ReaderUtils.getChildText(featureType, "title");
    }

    public String abstrct() {
        return ReaderUtils.getChildText(featureType, "abstract");
    }

    public List<String> keywords() {
        String raw = ReaderUtils.getChildText(featureType, "keywords");
        if (raw == null || "".equals(raw)) {
            return new ArrayList<String>();
        }
        StringTokenizer st = new StringTokenizer(raw, ", ");
        ArrayList keywords = new ArrayList();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }

        return keywords;
    }

    public List<Map<String, String>> metadataLinks() {
        ArrayList links = new ArrayList();
        Element metadataLinks = ReaderUtils.getChildElement(featureType, "metadataLinks");
        if (metadataLinks != null) {
            Element[] metadataLink = ReaderUtils.getChildElements(metadataLinks, "metadataLink");
            for (Element e : metadataLink) {
                HashMap m = new HashMap();
                m.put("metadataType", e.getAttribute("metadataType"));
                m.put("type", e.getAttribute("type"));
                if (e.getFirstChild() != null) {
                    m.put(null, e.getFirstChild().getNodeValue());
                }
                links.add(m);
            }
        }

        return links;
    }

    public Envelope latLonBoundingBox() throws Exception {
        Element box = ReaderUtils.getChildElement(featureType, "latLonBoundingBox");
        double minx = ReaderUtils.getDoubleAttribute(box, "minx", true);
        double miny = ReaderUtils.getDoubleAttribute(box, "miny", true);
        double maxx = ReaderUtils.getDoubleAttribute(box, "maxx", true);
        double maxy = ReaderUtils.getDoubleAttribute(box, "maxy", true);

        return new Envelope(minx, maxx, miny, maxy);
    }

    public Envelope nativeBoundingBox() throws Exception {
        Element box = ReaderUtils.getChildElement(featureType, "nativeBBox");
        boolean dynamic = ReaderUtils.getBooleanAttribute(box, "dynamic", false, true);
        if (dynamic) {
            return null;
        }

        double minx = ReaderUtils.getDoubleAttribute(box, "minx", true);
        double miny = ReaderUtils.getDoubleAttribute(box, "miny", true);
        double maxx = ReaderUtils.getDoubleAttribute(box, "maxx", true);
        double maxy = ReaderUtils.getDoubleAttribute(box, "maxy", true);

        return new Envelope(minx, maxx, miny, maxy);
    }

    public String defaultStyle() throws Exception {
        Element styles = ReaderUtils.getChildElement(featureType, "styles");
        return ReaderUtils.getAttribute(styles, "default", false);
    }

    public List<String> styles() throws Exception {
        Element styleRoot = ReaderUtils.getChildElement(featureType, "styles");
        if (styleRoot != null) {
            List<String> styleNames = new ArrayList<String>();
            Element[] styles = ReaderUtils.getChildElements(styleRoot, "style");
            for (Element style : styles) {
                styleNames.add(style.getTextContent().trim());
            }
            return styleNames;
        } else {
            return Collections.emptyList();
        }
    }

    public Map<String, Object> legendURL() throws Exception {
        Element legendURL = ReaderUtils.getChildElement(featureType, "LegendURL");

        if (legendURL != null) {
            HashMap map = new HashMap();
            map.put("width", Integer.parseInt(ReaderUtils.getAttribute(legendURL, "width", true)));
            map.put(
                    "height",
                    Integer.parseInt(ReaderUtils.getAttribute(legendURL, "height", true)));
            map.put("format", ReaderUtils.getChildText(legendURL, "Format", true));
            map.put(
                    "onlineResource",
                    ReaderUtils.getAttribute(
                            ReaderUtils.getChildElement(legendURL, "OnlineResource", true),
                            "xlink:href",
                            true));
            return map;
        }

        return null;
    }

    public boolean cachingEnabled() {
        Element cacheInfo = ReaderUtils.getChildElement(featureType, "cacheinfo");
        if (cacheInfo != null) {
            try {
                return "true".equals(ReaderUtils.getAttribute(cacheInfo, "enabled", false));
            } catch (Exception e) {
            }
        }
        return false;
    }

    public String cacheAgeMax() {
        Element cacheInfo = ReaderUtils.getChildElement(featureType, "cacheinfo");
        if (cacheInfo != null) {
            try {
                return ReaderUtils.getAttribute(cacheInfo, "maxage", false);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public boolean searchable() {
        Element searchable = ReaderUtils.getChildElement(featureType, "searchable");
        if (searchable != null) {
            try {
                return "true".equals(ReaderUtils.getAttribute(searchable, "enabled", false));
            } catch (Exception e) {
            }
        }

        return false;
    }

    public String regionateAttribute() {
        Element regionateAttribute = ReaderUtils.getChildElement(featureType, "regionateAttribute");
        if (regionateAttribute != null) {
            return regionateAttribute.getAttribute("value");
        }

        return null;
    }

    public String regionateStrategy() {
        Element regionateStrategy = ReaderUtils.getChildElement(featureType, "regionateStrategy");
        if (regionateStrategy != null) {
            return regionateStrategy.getAttribute("value");
        }

        return null;
    }

    public int regionateFeatureLimit() {
        Element regionateFeatureLimit =
                ReaderUtils.getChildElement(featureType, "regionateFeatureLimit");
        try {
            return Integer.valueOf(regionateFeatureLimit.getAttribute("value"));
        } catch (Exception e) {
            return 10;
        }
    }

    public int maxFeatures() {
        Element maxFeatures = ReaderUtils.getChildElement(featureType, "maxFeatures");
        try {
            return Integer.valueOf(maxFeatures.getTextContent());
        } catch (Exception e) {
            return 0;
        }
    }

    public String wmsPath() {
        return ReaderUtils.getChildText(featureType, "wmspath");
    }

    public String parentDirectoryName() {
        return parentDirectory.name();
    }
}
