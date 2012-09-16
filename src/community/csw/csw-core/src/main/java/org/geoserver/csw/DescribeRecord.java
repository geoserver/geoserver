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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.DescribeRecordType;

import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;

/**
 * Runs the DescribeRecord request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeRecord {

    static final Logger LOGGER = Logging.getLogger(DescribeRecord.class);
    
    /**
     * In case we make the schema languages pluggable we'll have to check what we actually
     * support, for the moment here is a set of different ways to refer to XML schema
     */
    static final Set<String> SUPPORTED_SCHEMA_LANGUAGES = new HashSet<String>() {
        {
            add("XMLSCHEMA");
            add("http://www.w3.org/2001/XMLSchema");
            add("http://www.w3.org/XML/Schema");
        }
    };

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
            // check we are not asked for a schema language we do not support
            if(request.getSchemaLanguage() != null 
                    && !SUPPORTED_SCHEMA_LANGUAGES.contains(request.getSchemaLanguage())) {
                throw new ServiceException("Unsupported schema language " + request.getSchemaLanguage(), 
                        ServiceException.INVALID_PARAMETER_VALUE, "schemaLanguage");
            }
            
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

                // we could be left with some extra feature types, the spec says we should not
                // complain and just return the ones we have (eventually an empty document)
                if (requested.size() != 0) {
                    LOGGER.log(Level.FINE, "Failed to locate feature types " + requested + ", ignoring them");
                }
                
                return (FeatureType[]) result.toArray(new FeatureType[result.size()]);
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to retrieve the feature type schemas",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
