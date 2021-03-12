/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.request.JSONPathVisitor;
import org.opengis.feature.type.FeatureType;

/**
 * Simplified JSON mapper that assumes we're referencing directly properties inside
 * features.properties
 */
public class STACPathVisitor extends JSONPathVisitor {

    public STACPathVisitor(FeatureType type) {
        super(type);
    }

    @Override
    public Object findFunction(TemplateBuilder builder, List<String> pathElements) {
        List<String> fullPath = new ArrayList<>();
        fullPath.add("features");
        fullPath.add("properties");
        fullPath.addAll(pathElements);
        return super.findFunction(builder, fullPath);
    }
}
