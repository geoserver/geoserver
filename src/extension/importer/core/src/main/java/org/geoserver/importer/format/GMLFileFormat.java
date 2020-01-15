/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransform;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geotools.data.FeatureReader;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml3.v3_2.GML;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.factory.Hints;
import org.geotools.wfs.v1_0.WFSConfiguration_1_0;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.PullParser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Supports reading GML simple features from a file with ".gml" extension
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GMLFileFormat extends VectorFormat {

    private static final Class[] TYPE_GUESS_TARGETS =
            new Class[] {Integer.class, Long.class, Double.class, Boolean.class, Date.class};

    private static final HashSet<Class> VALID_ATTRIBUTE_TYPES =
            new HashSet<>(
                    Arrays.asList(
                            (Class) Geometry.class,
                            Number.class,
                            Date.class,
                            Boolean.class,
                            String.class));

    private static final List<String> GML_ATTRIBUTES =
            Arrays.asList("name", "description", "boundedBy", "location");

    private static final Map<Class, Class> TYPE_PROMOTIONS =
            new HashMap<Class, Class>() {
                {
                    put(Integer.class, Long.class);
                    put(Long.class, Double.class);
                }
            };

    private static final String GML_VERSION_KEY = "version";

    private static final String GENERIC_2D_CODE = "EPSG:404000";

    enum GMLVersion {
        // use the wfs configurations, as they contain the gml ones
        GML2(new WFSConfiguration_1_0()),
        GML3(new org.geotools.wfs.v1_1.WFSConfiguration()),
        GML32(new org.geotools.wfs.v2_0.WFSConfiguration());

        Configuration configuration;

        private GMLVersion(Configuration configuration) {
            this.configuration = configuration;
        }

        public Configuration getConfiguration() {
            return configuration;
        }
    };

    @Override
    public FeatureReader read(ImportData data, ImportTask task) throws IOException {
        File file = getFileFromData(data);

        // we need to get the feature type, to use for the particular parse through the file
        // since we put it on the metadata from the list method, we first check if that's still
        // available
        SimpleFeatureType ft = task.getFeatureType();
        GMLVersion version = (GMLVersion) task.getMetadata().get(GML_VERSION_KEY);
        if (version == null) {
            version = GMLVersion.GML3;
        }
        if (ft == null) {
            FeatureTypeInfo fti = (FeatureTypeInfo) task.getLayer().getResource();
            ft = buildFeatureTypeFromInfo(fti);
        }
        return new GMLReader(new FileInputStream(file), version.getConfiguration(), ft);
    }

    @Override
    public void dispose(FeatureReader reader, ImportTask item) throws IOException {
        reader.close();
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        File file = getFileFromData(data);
        SimpleFeatureType featureType = getSchema(file);
        CatalogFactory factory = catalog.getFactory();
        CatalogBuilder cb = new CatalogBuilder(catalog);

        String name = featureType.getName().getLocalPart();
        FeatureTypeInfo ftinfo = factory.createFeatureType();
        ftinfo.setEnabled(true);
        ftinfo.setNativeName(name);
        ftinfo.setName(name);
        ftinfo.setTitle(name);
        ftinfo.setNamespace(catalog.getDefaultNamespace());
        List<AttributeTypeInfo> attributes = ftinfo.getAttributes();
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            AttributeTypeInfo att = factory.createAttribute();
            att.setName(ad.getLocalName());
            att.setBinding(ad.getType().getBinding());
            attributes.add(att);
        }

        LayerInfo layer = cb.buildLayer((ResourceInfo) ftinfo);
        ResourceInfo resource = layer.getResource();
        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = crs;
        if (crs == null) {
            resource.setSRS(GENERIC_2D_CODE);
            resource.setNativeCRS(null);
        } else {
            Integer code = null;
            try {
                code = CRS.lookupEpsgCode(crs, true);
            } catch (FactoryException e) {
                throw (IOException) new IOException().initCause(e);
            }

            try {
                // if we could not find a code, reproject to a target CRS
                if (code == null) {
                    targetCRS = CRS.decode("EPSG:4326", true);
                    resource.setSRS("EPSG:4326");
                    resource.setNativeCRS(crs);
                } else if (CRS.getAxisOrder(crs) == AxisOrder.NORTH_EAST) {
                    targetCRS = CRS.decode("EPSG:" + code, true);
                    resource.setSRS("EPSG:" + code);
                    resource.setNativeCRS(targetCRS);
                } else {
                    resource.setSRS("EPSG:" + code);
                    resource.setNativeCRS(crs);
                }
            } catch (Exception e) {
                throw new IOException("Failed to setup the layer CRS", e);
            }
        }
        resource.setNativeBoundingBox(EMPTY_BOUNDS);
        resource.setLatLonBoundingBox(EMPTY_BOUNDS);
        resource.getMetadata().put("recalculate-bounds", Boolean.TRUE);

        ImportTask task = new ImportTask(data);
        task.setLayer(layer);
        task.setFeatureType(featureType);
        task.getMetadata().put(GML_VERSION_KEY, featureType.getUserData().get(GML_VERSION_KEY));

        // in case the native CRS was not usable
        if (targetCRS != crs) {
            ReprojectTransform transform = new ReprojectTransform(crs, targetCRS);
            TransformChain<VectorTransform> chain = new VectorTransformChain(transform);
            task.setTransform(chain);
        }

        return Collections.singletonList(task);
    }

    SimpleFeatureType getSchema(File file) throws IOException {
        // do we have a schema location?
        boolean hasSchema = false;
        GMLVersion version = GMLVersion.GML3;
        try (FileReader input = new FileReader(file)) {
            // create a pull parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            // parse root element
            XmlPullParser parser = factory.newPullParser();

            // parser.setInput(input, "UTF-8");
            parser.setInput(input);
            parser.nextTag();

            String location =
                    parser.getAttributeValue(
                            "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
            hasSchema = location != null;

            String gmlNamespace = parser.getNamespace("gml");
            if (GML.NAMESPACE.equals(gmlNamespace)) {
                version = GMLVersion.GML32;
            } else {
                // missing, or the generic "http://opengis.net/gml" used for GML 3.1 and 2.x grrr
                // try to use some version detection based on heuristics (e.g., tags that
                // we know are specific to a particular version). These could certainly use some
                // improvement...
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String tag = parser.getName();
                    if ("outerBoundaryIs".equals(tag) || "innerBoundaryIs".equals(tag)) {
                        version = GMLVersion.GML2;
                        break;
                    }
                }
            }

        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse the input file", e);
        }

        // parse the xml and figure out the feature type
        String typeName = null;
        Map<String, AttributeDescriptor> guessedTypes = new HashMap<>();
        SimpleFeatureType result = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            SimpleFeature sf = null;
            PullParser parser =
                    new PullParser(version.getConfiguration(), fis, SimpleFeature.class);
            sf = (SimpleFeature) parser.parse();
            while (sf != null) {
                if (hasSchema) {
                    // we trust the feature type found by the parser then, but we still
                    // have to figure out the CRS
                    result = sf.getFeatureType();
                    if (result.getCoordinateReferenceSystem() == null) {
                        Geometry g = (Geometry) sf.getDefaultGeometry();
                        if (g != null && g.getUserData() instanceof CoordinateReferenceSystem) {
                            CoordinateReferenceSystem crs =
                                    (CoordinateReferenceSystem) g.getUserData();
                            result = FeatureTypes.transform(result, crs);
                        }
                    }
                }

                // even if we have the schema, we figure out the attributes anyways
                // since we need to decide what to do of the GML base ones
                if (typeName == null) {
                    typeName = sf.getFeatureType().getTypeName();
                }
                for (AttributeDescriptor ad : sf.getFeatureType().getAttributeDescriptors()) {
                    String name = ad.getLocalName();
                    updateSimpleTypeGuess(name, sf.getAttribute(name), guessedTypes);
                }

                // move to next feature
                sf = (SimpleFeature) parser.parse();
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse GML data", e);
        }

        // did we use the features own feature types?
        if (result != null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(result);
            for (String gmlAttribute : GML_ATTRIBUTES) {
                if (!guessedTypes.containsKey(gmlAttribute) && tb.get(gmlAttribute) != null) {
                    tb.remove(gmlAttribute);
                }
            }
            for (AttributeDescriptor ad : result.getAttributeDescriptors()) {
                String name = ad.getLocalName();
                Class<?> binding = ad.getType().getBinding();
                boolean valid = false;
                for (Class validAttributeType : VALID_ATTRIBUTE_TYPES) {
                    if (validAttributeType.isAssignableFrom(binding)) {
                        valid = true;
                        break;
                    }
                }

                if (!valid && tb.get(name) != null) {
                    tb.remove(name);
                }
            }

            result = tb.buildFeatureType();
        }

        // ok, the gml was schema-less and we figured out the type structure with heuristics then
        if (typeName != null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName(typeName);
            for (AttributeDescriptor ad : guessedTypes.values()) {
                tb.add(ad);
            }
            result = tb.buildFeatureType();
        } else {
            // uh oh, empty GML file?
            throw new IllegalArgumentException("Could not find any GML feature in the file");
        }

        result.getUserData().put(GML_VERSION_KEY, version);

        return result;
    }

    private void updateSimpleTypeGuess(
            String name, Object value, Map<String, AttributeDescriptor> guessedTypes) {
        if (value == null) {
            return;
        }

        // if we have already established it's a string, bail out
        AttributeDescriptor ad = guessedTypes.get(name);
        Class target = null;
        if (ad != null) {
            target = ad.getType().getBinding();
        }
        Class originalTarget = target;
        if (String.class.equals(target) || Geometry.class.equals(target)) {
            return;
        }

        if (value instanceof Geometry) {
            // for geometries, special case as we need to handle the CRS and we basically
            // have no promotions, either all equal, or all Geometry.class
            if (target == null) {
                Geometry g = (Geometry) value;
                AttributeTypeBuilder typeBuilder = new AttributeTypeBuilder();
                typeBuilder.setName(name);
                typeBuilder.setBinding(value.getClass());
                if (g.getUserData() instanceof CoordinateReferenceSystem) {
                    typeBuilder.setCRS((CoordinateReferenceSystem) g.getUserData());
                }
                AttributeDescriptor geometryDescriptor = typeBuilder.buildDescriptor(name);
                guessedTypes.put(name, geometryDescriptor);
            } else if (Geometry.class.isAssignableFrom(target) && !target.isInstance(value)) {
                AttributeTypeBuilder typeBuilder = new AttributeTypeBuilder();
                typeBuilder.init(ad);
                typeBuilder.setBinding(Geometry.class);
                AttributeDescriptor geometryDescriptor = typeBuilder.buildDescriptor(name);
                guessedTypes.put(name, geometryDescriptor);
            }
        } else {
            Hints hints = new Hints(ConverterFactory.SAFE_CONVERSION, true);
            if (target == null) {
                for (Class c : TYPE_GUESS_TARGETS) {
                    Object converted = Converters.convert(value, c, hints);
                    if (converted != null) {
                        target = c;
                        break;
                    }
                }

                if (target == null) {
                    target = String.class;
                }
            }

            // verify the current value is compatible with the target type
            Object converted = Converters.convert(value, target, hints);
            while (converted == null && TYPE_PROMOTIONS.get(target) != null) {
                target = TYPE_PROMOTIONS.get(target);
                converted = Converters.convert(value, target, hints);
            }
            // if all fails, use string
            if (converted == null) {
                target = String.class;
            }
            if (originalTarget != target) {
                AttributeTypeBuilder typeBuilder = new AttributeTypeBuilder();
                typeBuilder.setName(name);
                typeBuilder.setBinding(target);
                AttributeDescriptor newDescriptor = typeBuilder.buildDescriptor(name);
                guessedTypes.put(name, newDescriptor);
            }
        }
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask item) throws IOException {
        return -1;
    }

    @Override
    public String getName() {
        return "GML";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        File file = getFileFromData(data);
        return file.canRead() && "gml".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()));
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // no store support for GML
        return null;
    }
}
