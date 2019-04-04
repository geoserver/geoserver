/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import org.geotools.data.simple.SimpleFeatureCollection;

/** Base class for collection wrappers used to decide the output format in the controller code */
public abstract class FormatCollectionWrapper {

    SimpleFeatureCollection collection;

    public FormatCollectionWrapper(SimpleFeatureCollection collection) {
        this.collection = collection;
    }

    public SimpleFeatureCollection getCollection() {
        return collection;
    }

    public static class XMLCollectionWrapper extends FormatCollectionWrapper {

        public XMLCollectionWrapper(SimpleFeatureCollection collection) {
            super(collection);
        }
    }

    public static class JSONCollectionWrapper extends FormatCollectionWrapper {

        public JSONCollectionWrapper(SimpleFeatureCollection collection) {
            super(collection);
        }
    }
}
