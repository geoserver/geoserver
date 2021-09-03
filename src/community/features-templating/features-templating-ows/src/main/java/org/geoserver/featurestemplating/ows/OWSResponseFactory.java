/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows;

import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.ows.wfs.BaseTemplateGetFeatureResponse;
import org.geoserver.featurestemplating.ows.wfs.GMLTemplateResponse;
import org.geoserver.featurestemplating.ows.wfs.GeoJSONTemplateGetFeatureResponse;
import org.geoserver.featurestemplating.ows.wfs.HTMLTemplateResponse;
import org.geoserver.featurestemplating.ows.wfs.JSONLDGetFeatureResponse;
import org.geoserver.featurestemplating.ows.wms.GMLTemplateFeatureInfo;
import org.geoserver.featurestemplating.ows.wms.GeoJSONTemplateFeatureInfo;
import org.geoserver.featurestemplating.ows.wms.HTMLTemplateFeatureInfo;
import org.geoserver.featurestemplating.ows.wms.JSONLDTemplateFeatureInfo;
import org.geoserver.featurestemplating.ows.wms.TemplateFeatureInfoOutputFormat;
import org.geoserver.featurestemplating.ows.wms.TemplateGetFeatureInfoResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoResponse;

/** This class provides method to create a supported Template Response objects. */
public class OWSResponseFactory {

    private GeoServer gs;
    private TemplateLoader loader;

    public static OWSResponseFactory getInstance() {
        return GeoServerExtensions.bean(OWSResponseFactory.class);
    }

    public OWSResponseFactory(GeoServer gs, TemplateLoader templateLoader) {
        this.gs = gs;
        this.loader = templateLoader;
    }

    /**
     * Return a {@link BaseTemplateGetFeatureResponse} in case the identifier matches one of the
     * supported format.
     *
     * @param identifier the {@link TemplateIdentifier} for which build the corresponding Response
     *     object.
     * @return a {@link BaseTemplateGetFeatureResponse} matching the {@link TemplateIdentifier}.
     */
    public BaseTemplateGetFeatureResponse getFeatureResponse(TemplateIdentifier identifier) {
        BaseTemplateGetFeatureResponse resp = null;
        if (identifier != null) {
            switch (identifier) {
                case JSON:
                case GEOJSON:
                    resp = new GeoJSONTemplateGetFeatureResponse(gs, loader, identifier);
                    break;
                case GML32:
                case GML31:
                case GML2:
                    resp = new GMLTemplateResponse(gs, loader, identifier);
                    break;
                case HTML:
                    resp = new HTMLTemplateResponse(gs, loader);
                    break;
                case JSONLD:
                    resp = new JSONLDGetFeatureResponse(gs, loader);
                    break;
            }
        }
        return resp;
    }

    /**
     * Return a {@link GetFeatureInfoResponse} supporting templating.
     *
     * @param identifier the {@link TemplateIdentifier} for which build the corresponding Response
     *     object.
     * @param origFormat the request output format.
     * @return a {@link GetFeatureInfoResponse} matching the {@link TemplateIdentifier} and
     *     origFormat param.
     */
    public GetFeatureInfoResponse featureInfoResponse(
            TemplateIdentifier identifier, String origFormat) {
        if (identifier == null) return null;
        TemplateFeatureInfoOutputFormat resp = null;
        if (identifier.equals(TemplateIdentifier.HTML))
            resp = new HTMLTemplateFeatureInfo(origFormat);
        else if (identifier.equals(TemplateIdentifier.GEOJSON)
                || identifier.equals(TemplateIdentifier.JSON)) {
            resp = new GeoJSONTemplateFeatureInfo(identifier, origFormat);
        } else if (isGML(identifier)) resp = new GMLTemplateFeatureInfo(identifier, origFormat);
        else if (identifier.equals(TemplateIdentifier.JSONLD))
            resp = new JSONLDTemplateFeatureInfo();

        WMS wms = GeoServerExtensions.bean(WMS.class);
        GetFeatureInfoResponse response = new TemplateGetFeatureInfoResponse(wms, resp);
        return response;
    }

    private boolean isGML(TemplateIdentifier identifier) {
        return identifier.equals(TemplateIdentifier.GML2)
                || identifier.equals(TemplateIdentifier.GML31)
                || identifier.equals(TemplateIdentifier.GML32);
    }
}
