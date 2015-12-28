/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.Collection;
import java.util.Locale;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.crs.DynamicCrsMapResource;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geotools.referencing.CRS;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.InternationalString;

public class SRSDescriptionPage extends GeoServerBasePage implements IHeaderContributor {

    private String jsSrs;

    private String jsBbox;

    private double jsMaxResolution;

    /**
     * Adds the call to the {@code initMap()} js function at the onload event of the body tag
     * 
     * @see org.apache.wicket.markup.html.IHeaderContributor#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
     */
    public void renderHead(IHeaderResponse headerResponse) {
        String onLoadJsCall = "initMap('" + jsSrs + "', " + jsBbox + ", " + jsMaxResolution + ")";
        headerResponse.renderOnLoadJavascript(onLoadJsCall);
    }

    public SRSDescriptionPage(PageParameters params) {

        // this two contributions should be relative to the root of the webbapp's context path
        add(HeaderContributor.forCss("openlayers/theme/default/style.css"));
        add(HeaderContributor.forJavaScript("openlayers/OpenLayers.js"));

        final Locale locale = getLocale();
        final String code = params.getString("code");
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

                double[] dst1;
                double[] dst2;
                double x1;
                double y1;
                double x2;
                double y2;
                try {
                    MathTransform tr = CRS.findMathTransform(CRS.decode("EPSG:4326"), crs, true);
                    dst1 = new double[tr.getTargetDimensions()];
                    dst2 = new double[tr.getTargetDimensions()];
                    double[] src1 = new double[tr.getSourceDimensions()];
                    src1[0] = westBoundLongitude;
                    src1[1] = southBoundLatitude;

                    double[] src2 = new double[tr.getSourceDimensions()];
                    src2[0] = eastBoundLongitude;
                    src2[1] = northBoundLatitude;
                    tr.transform(src1, 0, dst1, 0, 1);
                    tr.transform(src2, 0, dst2, 0, 1);

                    x1 = Math.min(dst1[0], dst2[0]);
                    y1 = Math.min(dst1[1], dst2[1]);
                    x2 = Math.max(dst1[0], dst2[0]);
                    y2 = Math.max(dst1[1], dst2[1]);
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
