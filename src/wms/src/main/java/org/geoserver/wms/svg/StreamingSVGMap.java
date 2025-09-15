/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import static org.apache.commons.text.StringEscapeUtils.escapeXml10;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

/**
 * Streaming SVG encoder (does not support styling)
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class StreamingSVGMap extends WebMap {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");

    /** the XML and SVG header */
    private static final String SVG_HEADER =
            """
            <?xml version="1.0" standalone="no"?>
            	\
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"\s
            	stroke="green"\s
            	fill="none"\s
            	stroke-width="0.1%"
            	stroke-linecap="round"
            	stroke-linejoin="round"
            	width="_width_"\s
            	height="_height_"\s
            	viewBox="_viewBox_"\s
            	preserveAspectRatio="xMidYMid meet">
            """;

    /** the SVG closing element */
    private static final String SVG_FOOTER = "</svg>\n";

    private SVGWriter writer;

    /** Creates a new EncodeSVG object. */
    public StreamingSVGMap(WMSMapContent mapContent) {
        super(mapContent);
    }

    public void encode(final OutputStream out) throws IOException {
        Envelope env = this.mapContent.getRenderingArea();
        this.writer = new SVGWriter(out, mapContent.getRenderingArea());
        writer.setMinCoordDistance(env.getWidth() / 1000);

        long t = System.currentTimeMillis();

        writeHeader();

        writeLayers();

        writer.write(SVG_FOOTER);

        this.writer.flush();
        t = System.currentTimeMillis() - t;
        LOGGER.info("SVG generated in " + t + " ms");
    }

    public String createViewBox() {
        Envelope referenceSpace = mapContent.getRenderingArea();
        String viewBox = writer.getX(referenceSpace.getMinX())
                + " "
                + (writer.getY(referenceSpace.getMinY()) - referenceSpace.getHeight())
                + " "
                + referenceSpace.getWidth()
                + " "
                + referenceSpace.getHeight();

        return viewBox;
    }

    private void writeHeader() throws IOException {
        // TODO: this does not write out the doctype definition, there should be
        // a configuration option wether to include it or not.
        String viewBox = createViewBox();
        String header = SVG_HEADER.replaceAll("_viewBox_", viewBox);
        header = header.replaceAll("_width_", String.valueOf(mapContent.getMapWidth()));
        header = header.replaceAll("_height_", String.valueOf(mapContent.getMapHeight()));
        writer.write(header);
    }

    private void writeDefs(SimpleFeatureType layer) throws IOException {
        GeometryDescriptor gtype = layer.getGeometryDescriptor();
        Class<?> geometryClass = gtype.getType().getBinding();

        if ((geometryClass == MultiPoint.class) || (geometryClass == Point.class)) {
            writePointDefs();
        }
    }

    private void writePointDefs() throws IOException {
        writer.write("<defs>\n\t<circle id='point' cx='0' cy='0' r='0.25%' fill='blue'/>\n</defs>\n");
    }

    /** @task TODO: respect layer filtering given by their Styles */
    private void writeLayers() throws IOException {
        List<Layer> layers = mapContent.layers();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        for (Layer layer : layers) {
            SimpleFeatureSource fSource = (SimpleFeatureSource) layer.getFeatureSource();
            SimpleFeatureType schema = fSource.getSchema();

            try {
                String defaultGeometry =
                        schema.getGeometryDescriptor().getName().getLocalPart();
                ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
                BBOX bboxFilter = ff.bbox(ff.property(defaultGeometry), renderingArea);

                Query bboxQuery = new Query(schema.getTypeName(), bboxFilter);
                Query definitionQuery = layer.getQuery();
                Query finalQuery = new Query(DataUtilities.mixQueries(definitionQuery, bboxQuery, "svgEncoder"));
                finalQuery.setHints(definitionQuery.getHints());
                finalQuery.setSortBy(definitionQuery.getSortBy());
                finalQuery.setStartIndex(definitionQuery.getStartIndex());

                LOGGER.fine("obtaining FeatureReader for " + schema.getTypeName());
                LOGGER.fine("got FeatureReader, now writing");

                String groupId = schema.getTypeName();

                String styleName = layer.getStyle().getName();

                writer.write("<g id=\"" + escapeXml10(groupId) + "\"");

                if (!styleName.startsWith("#")) {
                    writer.write(" class=\"" + escapeXml10(styleName) + "\"");
                }

                writer.write(">\n");

                writeDefs(schema);

                try (SimpleFeatureIterator featureReader =
                        fSource.getFeatures(finalQuery).features()) {
                    writer.writeFeatures(fSource.getSchema(), featureReader, styleName);
                    writer.write("</g>\n");
                }
            } catch (IOException ex) {
                throw ex;
            } catch (Throwable t) {
                LOGGER.warning("UNCAUGHT exception: " + t.getMessage());

                IOException ioe = new IOException("UNCAUGHT exception: " + t.getMessage());
                ioe.setStackTrace(t.getStackTrace());
                throw ioe;
            }
        }
    }
}
