/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.LinkedHashMap;
import java.util.Map;
import org.geoserver.csw.records.RecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * A map from record name to {@link RecordDescriptor} that includes some name matching leniency, if
 * name searched for is not namespace qualified, then a match on the local part is attempted too
 */
public class RecordDescriptorsMap extends LinkedHashMap<Name, RecordDescriptor> {
    private static final long serialVersionUID = 335115347101959746L;

    public RecordDescriptor get(Object key) {
        if (!(key instanceof Name)) {
            return null;
        }
        Name name = (Name) key;
        RecordDescriptor descriptor = super.get(key);
        if (descriptor == null && name.getNamespaceURI() == null) {
            // relaxed match, see if we can find the record without the namespace
            for (Map.Entry<Name, RecordDescriptor> entry : entrySet()) {
                if (entry.getKey().getLocalPart().equals(name.getLocalPart())) {
                    return entry.getValue();
                }
            }
        }

        return descriptor;
    }
}
