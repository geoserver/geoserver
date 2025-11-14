/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;

public class HelloDocument extends AbstractDocument {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sample {@link CloseableIterator} property, to test serialization of properties like the ones found in the
     * different CollectionsDocument classes
     */
    public CloseableIterator<String> getCollections() {
        Iterator<String> it = List.of("value1", "value2", "value3").iterator();
        return new CloseableIteratorAdapter<>(it);
    }
}
