/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

/**
 * Generates a FeatureInfoResponse of type text. This simply reports the attributes of the feature requested as a text
 * string. This class just performs the writeTo, the GetFeatureInfoDelegate and abstract feature info class handle the
 * rest.
 *
 * @author James Macgill, PSU
 * @version $Id$
 */
public class TextFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private WMS wms;

    public TextFeatureInfoOutputFormat(final WMS wms) {
        super("text/plain");
        this.wms = wms;
    }

    /**
     * Writes the feature information to the client in text/plain format.
     *
     * @see GetFeatureInfoOutputFormat#write
     */
    @Override
    @SuppressWarnings("PMD.CloseResource") // just a wrapper, actual output managed by servlet
    public void write(FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        // getRequest().getGeoServer().getCharSet());
        PrintWriter writer = new PrintWriter(osw);

        // DJB: this is to limit the number of features read - as per the spec
        // 7.3.3.7 FEATURE_COUNT
        int featuresPrinted = 0; // how many features we've actually printed
        // so far!

        int maxfeatures = request.getFeatureCount(); // will default to 1
        // if not specified
        // in the request

        try {
            final List collections = results.getFeature();

            // for each layer queried
            for (Object collection : collections) {
                FeatureCollection fr = (FeatureCollection) collection;
                try (FeatureIterator reader = fr.features()) {

                    boolean startFeat = true;
                    while (reader.hasNext()) {
                        Feature feature = reader.next();

                        if (startFeat) {
                            writer.println(
                                    "Results for FeatureType '" + fr.getSchema().getName() + "':");
                            startFeat = false;
                        }

                        if (featuresPrinted < maxfeatures) {
                            writer.println("--------------------------------------------");

                            if (feature instanceof SimpleFeature f) {
                                writeSimpleFeature(writer, f);
                            } else {
                                writer.println(feature.toString());
                            }
                        }

                        writer.println("--------------------------------------------");
                        featuresPrinted++;
                    }
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
            writer.println("Unable to generate information " + ife);
        }
        if (featuresPrinted == 0) {
            writer.println("no features were found");
        }

        writer.flush();
    }

    private void writeSimpleFeature(PrintWriter writer, SimpleFeature f) {
        SimpleFeatureType schema = f.getType();
        List<AttributeDescriptor> types = schema.getAttributeDescriptors();

        for (AttributeDescriptor descriptor : types) {
            final Name name = descriptor.getName();
            final Class<?> binding = descriptor.getType().getBinding();
            if (Geometry.class.isAssignableFrom(binding)) {
                // writer.println(types[j].getName() + " =
                // [GEOMETRY]");

                // DJB: changed this to print out WKT - its very
                // nice for users
                // Geometry g = (Geometry)
                // f.getAttribute(types[j].getName());
                // writer.println(types[j].getName() + " =
                // [GEOMETRY] = "+g.toText() );

                // DJB: decided that all the geometry info was
                // too much - they should use GML version if
                // they want those details
                Geometry g = (Geometry) f.getAttribute(name);
                if (g != null) {
                    writer.println(
                            name + " = [GEOMETRY (" + g.getGeometryType() + ") with " + g.getNumPoints() + " points]");
                } else {
                    // GEOS-6829
                    writer.println(name + " = null");
                }
            } else if (Date.class.isAssignableFrom(binding) && TemporalUtils.isDateTimeFormatEnabled()) {
                // Temporal types print handling
                String printValue = TemporalUtils.printDate((Date) f.getAttribute(name));
                writer.println(name + " = " + printValue);
            } else {
                writer.println(name + " = " + f.getAttribute(name));
            }
        }
    }

    @Override
    public String getCharset() {
        return wms.getGeoServer().getSettings().getCharset();
    }
}
