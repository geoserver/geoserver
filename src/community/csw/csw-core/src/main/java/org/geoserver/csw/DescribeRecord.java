/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.DescribeRecordType;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.type.FeatureType;

/**
 * Runs the DescribeRecord request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeRecord {

    CSWInfo csw;

    CatalogStore store;

    public DescribeRecord(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;
    }

    /**
     * Returns the requested feature types
     * 
     * @param request
     * @return
     */
    public FeatureType[] run(DescribeRecordType request) {
        try {
            if (request.getTypeName() == null || request.getTypeName().isEmpty()) {
                // return all the ones we have
                return store.getRecordSchemas();
            } else {
                List<FeatureType> result = new ArrayList<FeatureType>();
                Set<QName> requested = new HashSet(request.getTypeName());
                FeatureType[] schemas = store.getRecordSchemas();
                for (FeatureType featureType : schemas) {
                    // convert the feature type name to a QName and check if it was requested
                    QName typeName = new QName(featureType.getName().getNamespaceURI(), featureType
                            .getName().getLocalPart());
                    if (requested.remove(typeName)) {
                        result.add(featureType);
                    }
                }

                // if we still have some elements it means we don't have some of the requested
                // schemas
                if (requested.size() != 0) {
                    throw new ServiceException("Failed to find feature types: " + requested,
                            ServiceException.INVALID_PARAMETER_VALUE, "typename");
                }

                return (FeatureType[]) result.toArray(new FeatureType[result.size()]);
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to retrieve the feature type schemas",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
