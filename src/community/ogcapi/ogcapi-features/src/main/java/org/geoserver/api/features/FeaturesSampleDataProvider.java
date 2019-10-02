/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import java.util.Collections;
import java.util.List;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.api.SampleDataProvider;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.stereotype.Component;

@Component
public class FeaturesSampleDataProvider implements SampleDataProvider {

    private final FeatureService service;

    FeaturesSampleDataProvider(FeatureService service) {
        this.service = service;
    }

    @Override
    public List<Link> getSampleData(LayerInfo layer) {
        if (layer.getResource() instanceof ResourceInfo && service.getService().isEnabled()) {
            String resourceId = layer.getResource().prefixedName();
            return APIRequestInfo.get()
                    .getLinksFor(
                            "ogc/features/collections/"
                                    + ResponseUtils.urlEncode(resourceId)
                                    + "/items",
                            FeaturesResponse.class,
                            resourceId + " items as ",
                            "data",
                            null,
                            "data",
                            false);
        }
        return Collections.emptyList();
    }
}
