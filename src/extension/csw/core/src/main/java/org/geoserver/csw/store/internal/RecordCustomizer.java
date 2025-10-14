/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.csw.DownloadLinkHandler;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geotools.api.data.CloseableIterator;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.ComplexAttributeImpl;

/** {@link FeatureCustomizer} subclass to deal with CSW DublinCore records */
public class RecordCustomizer extends FeatureCustomizer {

    private static final AttributeDescriptor REFERENCES_DESCRIPTOR;

    private static final AttributeDescriptor VALUE_DESCRIPTOR;

    private static final String REFERENCES = "references";

    private static final String TYPENAME = "RecordType";

    static {
        REFERENCES_DESCRIPTOR = CSWRecordDescriptor.getDescriptor(REFERENCES);
        ComplexType referenceType = (ComplexType) REFERENCES_DESCRIPTOR.getType();
        VALUE_DESCRIPTOR = (AttributeDescriptor) referenceType.getDescriptor("value");
    }

    /** An instance of {@link DownloadLinkHandler}, used to deal with download links */
    private DownloadLinkHandler downloadLinkHandler;

    public void setDownloadLinkHandler(DownloadLinkHandler downloadLinkHandler) {
        this.downloadLinkHandler = downloadLinkHandler;
    }

    public RecordCustomizer() {
        super(TYPENAME);
    }

    @Override
    public void customizeFeature(Feature feature, CatalogInfo resource) {
        List<Property> newReferencesList = new ArrayList<>();
        String link = null;
        try {
            try (CloseableIterator<String> links = downloadLinkHandler.generateDownloadLinks(resource)) {
                if (links == null) {
                    // No need to update the feature
                    return;
                }
                while (links.hasNext()) {
                    link = links.next();
                    newReferencesList.add(createReferencesElement(link));
                }
            }

        } catch (IOException e) {
            // ignore it (happens only on close)
        }

        List<Property> propertyList = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Property> oldValues = (List<Property>) feature.getValue();
        Iterator<Property> oldValuesIterator = oldValues.iterator();
        boolean insertReferences = false;

        // Copy all previous elements, references included
        while (oldValuesIterator.hasNext()) {
            Property prop = oldValuesIterator.next();
            if (REFERENCES.equalsIgnoreCase(prop.getName().getLocalPart())) {
                insertReferences = true;
            } else if (insertReferences) {

                // link String should contain the last link generated
                // let's recycle it to generate the full download link
                newReferencesList.add(createReferencesElement(downloadLinkHandler.extractFullDownloadLink(link)));
                // append new references to the current collection
                // before going to other elements
                propertyList.addAll(newReferencesList);
                insertReferences = false;
            }
            propertyList.add(prop);
        }
        feature.setValue(propertyList);
    }

    /** Create a new references element for the link */
    private Property createReferencesElement(String link) {
        Property urlAttribute = new AttributeImpl(link, VALUE_DESCRIPTOR, null);

        // Setting references
        return new ComplexAttributeImpl(Collections.singletonList(urlAttribute), REFERENCES_DESCRIPTOR, null);
    }
}
