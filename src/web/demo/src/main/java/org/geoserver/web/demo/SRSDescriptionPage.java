/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import com.vividsolutions.jts.geom.Envelope;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.ows.URLMangler;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.crs.DynamicCrsMapResource;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.InternationalString;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

public class SRSDescriptionPage extends GeoServerBasePage implements IHeaderContributor {

    private String jsSrs;

    private String jsBbox;

    private String jsUnit;

    private double jsMaxResolution;

    /**
     * Adds the call to the {@code initMap()} js function at the onload event of the body tag
     * 
     */
    public void renderHead(IHeaderResponse headerResponse) {
        String onLoadJsCall = "initMap('" + jsSrs + "', '" + jsUnit + "', " + jsBbox + ", " + jsMaxResolution + ")";
        headerResponse.render(new OnDomReadyHeaderItem(onLoadJsCall));
    }

    public SRSDescriptionPage(PageParameters params) {

        // this two contributions should be relative to the root of the webbapp's context path
        add(new Behavior() {
            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                HttpServletRequest req = getGeoServerApplication().servletRequest(getRequest());
                String baseUrl = baseURL(req);

                response.render(new CssUrlReferenceHeaderItem(
                    buildURL(baseUrl, "openlayers3/ol.css", null, URLMangler.URLType.RESOURCE),
                    null, null));
                response.render(new JavaScriptUrlReferenceHeaderItem(
                    buildURL(baseUrl, "openlayers3/ol.js", null, URLMangler.URLType.RESOURCE),
                    null, false, "UTF-8", null));
            }
        });

        final Locale locale = getLocale();
        final String code = params.get("code").toString();
        add(new Label("code", code));
        String name = "";
        try {
            name = CRS.getAuthorityFactory(true).getDescriptionText(code).toString(getLocale());
        } catch (Exception e) {
            //
        }

        String wkt = "";

        add(new Label("crsName", name));
        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode(code);
        } catch (Exception e) {
            wkt = "Error decoding CRS: " + e.getMessage();
        }

        InternationalString scope = null;
        InternationalString remarks = null;
        StringBuilder aovCoords = new StringBuilder();
        String areaOfValidity = "";
        this.jsBbox = "null";
        this.jsSrs = code;

        // use the unicode escape sequence for the degree sign so its not
        // screwed up by different local encodings
        this.jsUnit = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            if ("ft".equals(unit) || "feets".equals(unit))
                this.jsUnit = "feet";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }

        CoordinateReferenceSystem mapCrs = crs;
        if (crs != null) {
            // CoordinateSystem coordinateSystem = crs.getCoordinateSystem();
            // coordinateSystem.getName();
            // coordinateSystem.getRemarks();
            // coordinateSystem.getDimension();
            //
            // if(crs instanceof SingleCRS){
            // Datum datum = ((SingleCRS)crs).getDatum();
            // datum.getName();
            // datum.getAlias();
            // datum.getAnchorPoint();
            // datum.getRemarks();
            // datum.getScope();
            // }

            scope = crs.getScope();
            remarks = crs.getRemarks();

            wkt = crs.toString();
            Extent domainOfValidity = crs.getDomainOfValidity();
            if (domainOfValidity != null) {
                areaOfValidity = domainOfValidity.getDescription() == null ? "" : domainOfValidity
                        .getDescription().toString(locale);
                Collection<? extends GeographicExtent> geographicElements = domainOfValidity
                        .getGeographicElements();
                for (GeographicExtent ex : geographicElements) {
                    aovCoords.append(" ").append(ex);
                }
                // Envelope envelope = CRS.getEnvelope(crs);
                // jsBbox = "[" + envelope.getMinimum(0) + "," + envelope.getMinimum(1) + ","
                // + envelope.getMaximum(0) + "," + envelope.getMaximum(1) + "]";
                //
                // jsMaxResolution = getMaxResolution(envelope);

                // GeographicBoundingBox box = CRS.getGeographicBoundingBox(crs);
                // jsBbox = "[" + box.getWestBoundLongitude() + "," + box.getSouthBoundLatitude()
                // + "," + box.getEastBoundLongitude() + "," + box.getNorthBoundLatitude()
                // + "]";

                GeographicBoundingBox box = CRS.getGeographicBoundingBox(crs);


                double westBoundLongitude = box.getWestBoundLongitude();
                double eastBoundLongitude = box.getEastBoundLongitude();
                double southBoundLatitude = box.getSouthBoundLatitude();
                double northBoundLatitude = box.getNorthBoundLatitude();

                double x1;
                double y1;
                double x2;
                double y2;
                try {
                    Envelope envelope = new Envelope(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
                    MathTransform tr = CRS.findMathTransform(CRS.decode("EPSG:4326"), crs, true);
                    Envelope destEnvelope = JTS.transform(envelope, null, tr, 10);

                    x1 = destEnvelope.getMinX();
                    y1 = destEnvelope.getMinY();
                    x2 = destEnvelope.getMaxX();
                    y2 = destEnvelope.getMaxY();
                } catch (Exception e1) {
                    x1 = westBoundLongitude;
                    y1 = southBoundLatitude;
                    x2 = eastBoundLongitude;
                    y2 = northBoundLatitude;
                    this.jsSrs = "EPSG:4326";
                    try {
                        mapCrs = CRS.decode("EPSG:4326");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                String bbox = "[" + x1 + "," + y1 + "," + x2 + "," + y2 + "]";
                this.jsBbox = bbox;

                double width = x2 - x1;
                double height = y2 - y1;
                double maxres = getMaxResolution(width, height);
                this.jsMaxResolution = maxres;
            }
        }

        add(new Label("crsScope", scope == null ? "-" : scope.toString(locale)));
        add(new Label("crsRemarks", remarks == null ? "-" : remarks.toString(locale)));
        add(new Label("wkt", wkt));
        add(new Label("aovCoords", aovCoords.toString()));
        add(new Label("aovDescription", areaOfValidity));

        Image aovMap = new Image("aovMap", new DynamicCrsMapResource(mapCrs));
        add(aovMap);
        
        // link with the reprojection console
        add(new SimpleBookmarkableLink("reprojectFrom", ReprojectPage.class, new ParamResourceModel("reprojectFrom", this, code), "fromSRS", code));
        add(new SimpleBookmarkableLink("reprojectTo", ReprojectPage.class, new ParamResourceModel("reprojectTo", this, code), "toSRS", code));
    }

    private double getMaxResolution(final double w, final double h) {
        return 4 * (((w > h) ? w : h) / 256);
    }

}
