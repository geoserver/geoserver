/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.xsd.PullParser;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLRawReader implements Iterable<Object>, Iterator<Object> {

    private final PullParser parser;

    private Object next;

    public static enum ReadType {
        FEATURES,
        SCHEMA_AND_FEATURES
    }

    public KMLRawReader(InputStream inputStream) {
        this(inputStream, KMLRawReader.ReadType.FEATURES, null);
    }

    public KMLRawReader(InputStream inputStream, KMLRawReader.ReadType readType) {
        this(inputStream, readType, null);
    }

    public KMLRawReader(
            InputStream inputStream,
            KMLRawReader.ReadType readType,
            SimpleFeatureType featureType) {
        if (KMLRawReader.ReadType.SCHEMA_AND_FEATURES.equals(readType)) {
            if (featureType == null) {
                parser =
                        new PullParser(
                                new KMLConfiguration(), inputStream, KML.Placemark, KML.Schema);
            } else {
                parser =
                        new PullParser(
                                new KMLConfiguration(),
                                inputStream,
                                pullParserArgs(
                                        featureTypeSchemaNames(featureType),
                                        KML.Placemark,
                                        KML.Schema));
            }
        } else if (KMLRawReader.ReadType.FEATURES.equals(readType)) {
            if (featureType == null) {
                parser = new PullParser(new KMLConfiguration(), inputStream, KML.Placemark);
            } else {
                parser =
                        new PullParser(
                                new KMLConfiguration(),
                                inputStream,
                                pullParserArgs(featureTypeSchemaNames(featureType), KML.Placemark));
            }
        } else {
            throw new IllegalArgumentException("Unknown parse read type: " + readType.toString());
        }
        next = null;
    }

    private Object[] pullParserArgs(List<QName> featureTypeSchemaNames, Object... args) {
        Object[] parserArgs = new Object[featureTypeSchemaNames.size() + args.length];
        System.arraycopy(args, 0, parserArgs, 0, args.length);
        System.arraycopy(
                featureTypeSchemaNames.toArray(),
                0,
                parserArgs,
                args.length,
                featureTypeSchemaNames.size());
        return parserArgs;
    }

    @SuppressWarnings("unchecked")
    private List<QName> featureTypeSchemaNames(SimpleFeatureType featureType) {
        Map<Object, Object> userData = featureType.getUserData();
        if (userData.containsKey("schemanames")) {
            List<String> names = (List<String>) userData.get("schemanames");
            List<QName> qnames = new ArrayList<QName>(names.size());
            for (String name : names) {
                qnames.add(new QName(name));
            }
            return qnames;
        }
        return Collections.emptyList();
    }

    private Object read() throws IOException {
        Object parsedObject;
        try {
            parsedObject = parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        return parsedObject;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = read();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    @Override
    public Object next() {
        if (next != null) {
            Object result = next;
            next = null;
            return result;
        }
        Object feature;
        try {
            feature = read();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public Iterator<Object> iterator() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
