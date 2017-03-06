/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.FeatureTypeInfo;

public class ApplicationSchemaXSD2 extends ApplicationSchemaXSD1 {

    public ApplicationSchemaXSD2(FeatureTypeSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    public ApplicationSchemaXSD2(FeatureTypeSchemaBuilder schemaBuilder, Map<String, Set<FeatureTypeInfo>> featureTypes) {
        super(schemaBuilder, featureTypes);
    }

    @Override
    protected XSDSchema buildSchema() throws IOException {
        Set<FeatureTypeInfo> types = new HashSet();
        for (Set<FeatureTypeInfo> fts : featureTypes.values()) {
            types.addAll(fts);
        }
        return schemaBuilder.build(types.toArray(new FeatureTypeInfo[types.size()]), baseURL, 1);
    }
}
