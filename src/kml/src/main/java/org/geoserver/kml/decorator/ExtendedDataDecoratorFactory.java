/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;
import de.micromata.opengis.kml.v_2_2_0.SimpleField;
import java.util.Date;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Converter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.XmlConverterFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Adds schema and attributes to the KML output
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ExtendedDataDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {

        if (!context.isExtendedDataEnabled()) {
            return null;
        }

        if (Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkDataDecorator();
        } else if (Document.class.isAssignableFrom(featureClass)) {
            return new DocumentSchemaDecorator();
        }

        return null;
    }

    static class DocumentSchemaDecorator implements KmlDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Document doc = (Document) feature;

            // add a schema for each layer in the request (schemas have to be placed in the
            // Document, can't be placed in a Folder unfortunately
            int i = 1;
            for (SimpleFeatureType schema : context.getFeatureTypes()) {
                if (schema != null) {
                    String id = schema.getTypeName() + "_" + i;
                    addSchema(doc, id, schema);
                }
                i++;
            }

            return doc;
        }

        private void addSchema(Document doc, String id, SimpleFeatureType featureType) {
            Schema schema = doc.createAndAddSchema();
            schema.setId(id);
            schema.setName(id);
            for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
                // skip geometry attributes
                if (ad instanceof GeometryDescriptor) {
                    continue;
                }

                SimpleField field = schema.createAndAddSimpleField();
                field.setName(ad.getLocalName());
                field.setType(getKmlFieldType(ad));
            }
        }

        private String getKmlFieldType(AttributeDescriptor ad) {
            AttributeType at = ad.getType();
            if (Short.class.equals(at.getBinding())) {
                return "short";
            } else if (Integer.class.equals(at.getBinding())) {
                return "int";
            } else if (Float.class.equals(at.getBinding())) {
                return "float";
            } else if (Double.class.equals(at.getBinding())) {
                return "double";
            } else if (Boolean.class.equals(at.getBinding())) {
                return "bool";
            } else {
                return "string";
            }
        }
    }

    static class PlacemarkDataDecorator implements KmlDecorator {
        static final Logger LOGGER = Logging.getLogger(PlacemarkDataDecorator.class);
        static final Converter DATE_CONVERTER =
                new XmlConverterFactory().createConverter(Date.class, String.class, null);

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            SimpleFeature sf = context.getCurrentFeature();
            Placemark pm = (Placemark) feature;

            // create the extended data, and encode any non null, non geometric attribute
            ExtendedData exd = pm.createAndSetExtendedData();
            SchemaData schemaData = exd.createAndAddSchemaData();
            schemaData.setSchemaUrl(
                    "#"
                            + context.getCurrentFeatureType().getTypeName()
                            + "_"
                            + context.getCurrentLayerIndex());
            for (AttributeDescriptor ad : sf.getFeatureType().getAttributeDescriptors()) {
                // skip geometry attributes
                if (ad instanceof GeometryDescriptor) {
                    continue;
                }

                Object value = sf.getAttribute(ad.getLocalName());
                if (value == null) {
                    continue;
                }

                // make an exception for dates
                String kmlValue;
                if (value instanceof Date) {
                    try {
                        kmlValue = DATE_CONVERTER.convert(value, String.class);
                    } catch (Exception e) {
                        throw new ServiceException(
                                "Failed to convert date into string while "
                                        + "generating extended data section",
                                e);
                    }
                } else {
                    kmlValue = Converters.convert(value, String.class);
                }

                SimpleData sd = schemaData.createAndAddSimpleData(ad.getLocalName());
                sd.setValue(kmlValue);
            }

            return pm;
        }
    }
}
