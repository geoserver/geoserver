/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.response;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfstemplating.builders.geojson.GeoJsonRootBuilder;
import org.geoserver.wfstemplating.builders.impl.RootBuilder;
import org.geoserver.wfstemplating.configuration.TemplateConfiguration;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.geoserver.wfstemplating.writers.GeoJsonWriter;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;

/** Write a valid GeoJSON output from a template */
public class GeoJsonTemplateGetFeatureResponse extends BaseTemplateGetFeatureResponse {

    private GeoJSONGetFeatureResponse delegate;

    public GeoJsonTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, GeoJSONGetFeatureResponse delegate) {
        super(gs, configuration, TemplateIdentifier.GEOJSON);
        this.delegate = delegate;
    }

    @Override
    protected void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root) {
        GeoJsonRootBuilder rb = (GeoJsonRootBuilder) root;
        GeoJsonWriter jsonWriter = (GeoJsonWriter) writer;
        String strFlatOutput =
                rb.getVendorOption(RootBuilder.VendorOption.FLAT_OUTPUT.getVendorOptionName());
        boolean flatOutput = strFlatOutput != null ? Boolean.valueOf(strFlatOutput) : false;
        jsonWriter.setFlatOutput(flatOutput);
    }

    @Override
    public String getCapabilitiesElementName() {
        return delegate.getCapabilitiesElementName();
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return delegate.getMimeType(value, operation);
    }
}
