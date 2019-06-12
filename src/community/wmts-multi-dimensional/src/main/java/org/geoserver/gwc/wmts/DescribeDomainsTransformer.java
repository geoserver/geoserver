/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.wms.WMS;
import org.geotools.data.Query;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the describe domains operation. */
class DescribeDomainsTransformer extends TransformerBase {

    public DescribeDomainsTransformer(WMS wms) {
        setIndentation(2);
        setEncoding(wms.getCharSet());
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler);
    }

    class TranslatorSupport extends TransformerBase.TranslatorSupport {

        public TranslatorSupport(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object object) throws IllegalArgumentException {
            if (!(object instanceof Domains)) {
                throw new IllegalArgumentException(
                        "Expected domains info but instead got: "
                                + object.getClass().getCanonicalName());
            }
            Domains domains = (Domains) object;
            Attributes attributes =
                    createAttributes(
                            new String[] {
                                "xmlns",
                                "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd",
                                "xmlns:ows",
                                "http://www.opengis.net/ows/1.1",
                                "version",
                                "1.1"
                            });
            start("Domains", attributes);
            Map<String, Tuple<Integer, List<String>>> domainsValues = new HashMap<>();
            domains.getDimensions()
                    .forEach(
                            dimension -> {
                                Tuple<Integer, List<String>> dimensionValues =
                                        dimension.getDomainValuesAsStrings(
                                                new Query(null, domains.getFilter()),
                                                domains.getExpandLimit());
                                domainsValues.put(dimension.getDimensionName(), dimensionValues);
                            });
            if (domains.getSpatialDomain() != null && !domains.getSpatialDomain().isEmpty()) {
                handleBoundingBox(domains.getSpatialDomain());
            }
            domainsValues
                    .entrySet()
                    .forEach(
                            dimensionValues ->
                                    handleDimension(
                                            dimensionValues.getKey(), dimensionValues.getValue()));
            end("Domains");
        }

        private void handleBoundingBox(ReferencedEnvelope boundingBox) {
            if (boundingBox == null) {
                return;
            }
            start("SpaceDomain");
            CoordinateReferenceSystem crs = boundingBox.getCoordinateReferenceSystem();
            Attributes attributes =
                    createAttributes(
                            new String[] {
                                "CRS", crs == null ? "EPSG:4326" : CRS.toSRS(crs),
                                "minx", String.valueOf(boundingBox.getMinX()),
                                "miny", String.valueOf(boundingBox.getMinY()),
                                "maxx", String.valueOf(boundingBox.getMaxX()),
                                "maxy", String.valueOf(boundingBox.getMaxY()),
                            });
            element("BoundingBox", "", attributes);
            end("SpaceDomain");
        }

        private void handleDimension(
                String dimensionName, Tuple<Integer, List<String>> domainsValuesAsStrings) {
            start("DimensionDomain");
            element("ows:Identifier", dimensionName);
            element(
                    "Domain",
                    domainsValuesAsStrings.second.stream().collect(Collectors.joining(",")));
            element("Size", String.valueOf(domainsValuesAsStrings.first));
            end("DimensionDomain");
        }
    }
}
