/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wms;

import org.geoserver.featurestemplating.configuration.TemplateIdentifier;

public class GeoJSONTemplateFeatureInfo extends TemplateFeatureInfoOutputFormat {

    public GeoJSONTemplateFeatureInfo(TemplateIdentifier identifier, String origFormat) {

        super(identifier, origFormat);
    }
}
