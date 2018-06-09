/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.impl.XSDElementDeclarationImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.feature.FlatteningFeatureCollection;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.DateUtil;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "csv". The refence
 * specification for this format can be found in this RFC: http://www.rfc-editor.org/rfc/rfc4180.txt
 *
 * @author Justin Deoliveira, OpenGeo, jdeolive@opengeo.org
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org
 * @author Andrea Aime, OpenGeo
 */
public class CSVOutputFormat extends WFSGetFeatureOutputFormat {

    static final Pattern CSV_ESCAPES = Pattern.compile("[\"\n,\r]");

    public CSVOutputFormat(GeoServer gs) {
        // this is the name of your output format, it is the string
        // that will be used when requesting the format in a
        // GEtFeature request:
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super(gs, "csv");
    }

    /** @return "text/csv"; */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        // won't allow browsers to open it directly, but that's the mime
        // state in the RFC
        return "text/csv";
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFileName = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
        return outputFileName + ".csv";
    }

    /** @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation) */
    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        // write out content here

        // create a writer
        BufferedWriter w =
                new BufferedWriter(
                        new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset()));

        // get the feature collection
        FeatureCollection<?, ?> fc = featureCollection.getFeature().get(0);

        if (fc.getSchema() instanceof SimpleFeatureType) {
            // Flatten the collection if necessary (the request was a WFS 2.0 joining GetFeature
            // one, the features contain other SimpleFeature as attributes)
            fc = FlatteningFeatureCollection.flatten((SimpleFeatureCollection) fc);

            // write out the header
            SimpleFeatureType ft = (SimpleFeatureType) fc.getSchema();
            w.write("FID,");
            for (int i = 0; i < ft.getAttributeCount(); i++) {
                AttributeDescriptor ad = ft.getDescriptor(i);
                w.write(prepCSVField(ad.getLocalName()));

                if (i < ft.getAttributeCount() - 1) {
                    w.write(",");
                }
            }
        } else {
            // complex features
            w.write("gml:id,");

            int i = 0;
            for (PropertyDescriptor att : fc.getSchema().getDescriptors()) {
                // exclude temporary attributes
                if (!att.getName().getLocalPart().startsWith("FEATURE_LINK")) {
                    if (i > 0) {
                        w.write(",");
                    }
                    String elName = att.getName().toString();
                    Object xsd = att.getUserData().get(XSDElementDeclaration.class);
                    if (xsd != null && xsd instanceof XSDElementDeclarationImpl) {
                        // get the prefixed name if possible
                        // otherwise defaults to the full name with namespace URI
                        XSDElementDeclarationImpl xsdEl = (XSDElementDeclarationImpl) xsd;
                        elName = xsdEl.getQName();
                    }
                    w.write(prepCSVField(elName));
                    i++;
                }
            }
        }
        // by RFC each line is terminated by CRLF
        w.write("\r\n");

        // prepare the formatter for numbers
        NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
        coordFormatter.setMaximumFractionDigits(
                getInfo().getGeoServer().getSettings().getNumDecimals());
        coordFormatter.setGroupingUsed(false);

        // prepare the list of formatters
        AttrFormatter[] formatters = getFormatters(fc.getSchema());

        // write out the features
        FeatureIterator<?> i = fc.features();
        try {
            while (i.hasNext()) {
                Feature f = i.next();
                // dump fid
                w.write(prepCSVField(f.getIdentifier().getID()));
                w.write(",");
                if (f instanceof SimpleFeature) {
                    // dump attributes
                    for (int j = 0; j < ((SimpleFeature) f).getAttributeCount(); j++) {
                        Object att = ((SimpleFeature) f).getAttribute(j);
                        if (att != null) {
                            String value = formatters[j].format(att);
                            w.write(value);
                        }
                        if (j < ((SimpleFeature) f).getAttributeCount() - 1) {
                            w.write(",");
                        }
                    }
                } else {
                    // complex feature
                    Iterator<PropertyDescriptor> descriptors =
                            fc.getSchema().getDescriptors().iterator();

                    // dump attributes
                    int j = 0;
                    while (descriptors.hasNext()) {
                        PropertyDescriptor desc = descriptors.next();

                        if (desc.getName().getLocalPart().startsWith("FEATURE_LINK")) {
                            // skip temporary attributes
                            continue;
                        }
                        if (j > 0) {
                            w.write(",");
                        }
                        j++;
                        // Multi valued properties aren't supported, only for SF0 for now
                        Collection<Property> values = f.getProperties(desc.getName());
                        if (values.size() > 1) {
                            throw new UnsupportedOperationException(
                                    "Multi valued properties aren't supported with CSV format!");
                        }

                        Object att = null;
                        if (!values.isEmpty()) {
                            att = values.iterator().next().getValue();
                        }

                        if (att != null) {
                            String value = formatToString(att, coordFormatter);
                            w.write(prepCSVField(value));
                        }
                    }
                }
                // by RFC each line is terminated by CRLF
                w.write("\r\n");
            }
        } finally {
            i.close();
        }

        w.flush();
    }

    private AttrFormatter[] getFormatters(FeatureType schema) {
        if (schema instanceof SimpleFeatureType) {
            // prepare the formatter for numbers
            NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
            coordFormatter.setMaximumFractionDigits(
                    getInfo().getGeoServer().getSettings().getNumDecimals());
            coordFormatter.setGroupingUsed(false);

            SimpleFeatureType sft = (SimpleFeatureType) schema;
            AttrFormatter[] formatters = new AttrFormatter[sft.getAttributeCount()];
            int i = 0;
            for (AttributeDescriptor attributeDescriptor : sft.getAttributeDescriptors()) {
                Class<?> binding = attributeDescriptor.getType().getBinding();
                if (Number.class.isAssignableFrom(binding)) {
                    formatters[i] = new NumberFormatter(coordFormatter);
                } else if (java.sql.Date.class.isAssignableFrom(binding)) {
                    formatters[i] = sqlDateFormatter;
                } else if (java.sql.Time.class.isAssignableFrom(binding)) {
                    formatters[i] = sqlTimeFormatter;
                } else if (java.util.Date.class.isAssignableFrom(binding)) {
                    formatters[i] = juDateFormatter;
                } else {
                    formatters[i] = defaultFormatter;
                }
                i++;
            }
            return formatters;
        } else {
            return null;
        }
    }

    private interface AttrFormatter {
        String format(Object att);
    }

    private static class NumberFormatter implements AttrFormatter {
        private final NumberFormat coordFormatter;

        public NumberFormatter(NumberFormat coordFormatter) {
            this.coordFormatter = coordFormatter;
        }

        @Override
        public String format(Object att) {
            return coordFormatter.format(att);
        }
    }

    private static class JUDateFormatter implements AttrFormatter {
        @Override
        public String format(Object att) {
            return prepCSVField(DateUtil.serializeDateTime((Date) att));
        }
    }

    private static AttrFormatter juDateFormatter = new JUDateFormatter();

    private static class SQLDateFormatter implements AttrFormatter {
        @Override
        public String format(Object att) {
            return prepCSVField(DateUtil.serializeSqlDate((java.sql.Date) att));
        }
    }

    private static AttrFormatter sqlDateFormatter = new SQLDateFormatter();

    private static class SQLTimeFormatter implements AttrFormatter {
        @Override
        public String format(Object att) {
            return prepCSVField(DateUtil.serializeSqlTime((java.sql.Time) att));
        }
    }

    private static AttrFormatter sqlTimeFormatter = new SQLTimeFormatter();

    private static class DefaultFormatter implements AttrFormatter {
        @Override
        public String format(Object att) {
            return prepCSVField(att.toString());
        }
    }

    private static AttrFormatter defaultFormatter = new DefaultFormatter();

    private String formatToString(Object att, NumberFormat coordFormatter) {
        String value;
        if (att instanceof Number) {
            // don't allow scientific notation in the output, as OpenOffice won't
            // recognize that as a number
            value = coordFormatter.format(att);
        } else if (att instanceof Date) {
            // serialize dates in ISO format
            if (att instanceof java.sql.Date)
                value = DateUtil.serializeSqlDate((java.sql.Date) att);
            else if (att instanceof java.sql.Time)
                value = DateUtil.serializeSqlTime((java.sql.Time) att);
            else value = DateUtil.serializeDateTime((Date) att);
        } else {
            // everything else we just "toString"
            value = att.toString();
        }
        return value;
    }

    private static Escaper escaper = Escapers.builder().addEscape('"', "\"\"").build();

    /*
     * The CSV "spec" explains that fields with certain properties must be
     * delimited by double quotes, and also that double quotes within fields
     * must be escaped.  This method takes a field and returns one that
     * obeys the CSV spec.
     */
    private static String prepCSVField(String field) {
        // "embedded double-quote characters must be represented by a pair of double-quote
        // characters."
        String mod = escaper.escape(field);

        /*
         * Enclose string in double quotes if it contains double quotes, commas, or newlines
         */
        if (CSV_ESCAPES.matcher(mod).find()) {
            mod = "\"" + mod + "\"";
        }

        return mod;
    }

    @Override
    public String getCapabilitiesElementName() {
        return "CSV";
    }

    @Override
    public String getCharset(Operation operation) {
        return gs.getGlobal().getSettings().getCharset();
    }
}
