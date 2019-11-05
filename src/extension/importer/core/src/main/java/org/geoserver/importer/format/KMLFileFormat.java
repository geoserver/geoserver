/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.KMLPlacemarkTransform;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class KMLFileFormat extends VectorFormat {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static String KML_SRS = "EPSG:4326";

    public static CoordinateReferenceSystem KML_CRS;

    private static KMLPlacemarkTransform kmlTransform = new KMLPlacemarkTransform();

    static {
        try {
            KML_CRS = CRS.decode(KML_SRS);
        } catch (Exception e) {
            throw new RuntimeException("Could not decode: EPSG:4326", e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FeatureReader read(ImportData data, ImportTask task) throws IOException {
        File file = getFileFromData(data);

        // we need to get the feature type, to use for the particular parse through the file
        // since we put it on the metadata from the list method, we first check if that's still
        // available
        SimpleFeatureType ft = (SimpleFeatureType) task.getFeatureType();
        if (ft == null) {
            // if the type is not available, we can generate one from the resource
            // we aren't able to ask for the feature type from the resource directly,
            // because we don't have a backing store
            FeatureTypeInfo fti = (FeatureTypeInfo) task.getLayer().getResource();
            ft = buildFeatureTypeFromInfo(fti);
            MetadataMap metadata = fti.getMetadata();
            if (metadata.containsKey("importschemanames")) {
                Map<Object, Object> userData = ft.getUserData();
                userData.put("schemanames", metadata.get("importschemanames"));
            }
        }
        return read(ft, file);
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> read(
            SimpleFeatureType featureType, File file) {
        try {
            return new KMLTransformingFeatureReader(featureType, new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> read(
            SimpleFeatureType featureType, InputStream inputStream) {
        return new KMLTransformingFeatureReader(featureType, inputStream);
    }

    @Override
    public void dispose(@SuppressWarnings("rawtypes") FeatureReader reader, ImportTask task)
            throws IOException {
        reader.close();
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask task) throws IOException {
        // we don't have a fast way to get the count
        // instead of parsing through the entire file
        return -1;
    }

    @Override
    public String getName() {
        return "KML";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        File file = getFileFromData(data);
        return file.canRead() && "kml".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()));
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // null means no direct store imports can be performed
        return null;
    }

    public Collection<SimpleFeatureType> parseFeatureTypes(String typeName, File file)
            throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return parseFeatureTypes(typeName, inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private SimpleFeatureType unionFeatureTypes(SimpleFeatureType a, SimpleFeatureType b) {
        if (a == null) {
            return b;
        }
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.init(a);
        List<AttributeDescriptor> attributeDescriptors = a.getAttributeDescriptors();
        Set<String> attrNames = new HashSet<String>(attributeDescriptors.size());
        for (AttributeDescriptor ad : attributeDescriptors) {
            attrNames.add(ad.getLocalName());
        }
        for (AttributeDescriptor ad : b.getAttributeDescriptors()) {
            if (!attrNames.contains(ad.getLocalName())) {
                ftb.add(ad);
            }
        }
        return ftb.buildFeatureType();
    }

    public SimpleFeatureType convertParsedFeatureType(
            SimpleFeatureType ft, String name, Set<String> untypedAttributes) {
        SimpleFeatureType transformedType = kmlTransform.convertFeatureType(ft);
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.init(transformedType);
        Set<String> existringAttrNames = new HashSet<String>();
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            existringAttrNames.add(ad.getLocalName());
        }
        for (String attr : untypedAttributes) {
            if (!existringAttrNames.contains(attr)) {
                ftb.add(attr, String.class);
            }
        }
        ftb.setName(name);
        ftb.setCRS(KML_CRS);
        ftb.setSRS(KML_SRS);
        return ftb.buildFeatureType();
    }

    public List<SimpleFeatureType> parseFeatureTypes(String typeName, InputStream inputStream)
            throws IOException {
        KMLRawReader reader =
                new KMLRawReader(inputStream, KMLRawReader.ReadType.SCHEMA_AND_FEATURES);
        Set<String> untypedAttributes = new HashSet<String>();
        List<String> schemaNames = new ArrayList<String>();
        List<SimpleFeatureType> schemas = new ArrayList<SimpleFeatureType>();
        SimpleFeatureType aggregateFeatureType = null;
        for (Object object : reader) {
            if (object instanceof SimpleFeature) {
                SimpleFeature feature = (SimpleFeature) object;
                SimpleFeatureType ft = feature.getFeatureType();
                aggregateFeatureType = unionFeatureTypes(aggregateFeatureType, ft);
                Map<Object, Object> userData = feature.getUserData();
                @SuppressWarnings("unchecked")
                Map<String, Object> untypedData =
                        (Map<String, Object>) userData.get("UntypedExtendedData");
                if (untypedData != null) {
                    untypedAttributes.addAll(untypedData.keySet());
                }
            } else if (object instanceof SimpleFeatureType) {
                SimpleFeatureType schema = (SimpleFeatureType) object;
                schemas.add(schema);
                schemaNames.add(schema.getName().getLocalPart());
            }
        }
        if (aggregateFeatureType == null && schemas.isEmpty()) {
            throw new IllegalArgumentException("No features found");
        }
        SimpleFeatureType featureType = aggregateFeatureType;
        for (SimpleFeatureType schema : schemas) {
            featureType = unionFeatureTypes(featureType, schema);
        }
        featureType = convertParsedFeatureType(featureType, typeName, untypedAttributes);
        if (!schemaNames.isEmpty()) {
            Map<Object, Object> userData = featureType.getUserData();
            userData.put("schemanames", schemaNames);
        }
        return Collections.singletonList(featureType);
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        File file = getFileFromData(data);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        String baseName = typeNameFromFile(file);
        CatalogFactory factory = catalog.getFactory();

        Collection<SimpleFeatureType> featureTypes = parseFeatureTypes(baseName, file);
        List<ImportTask> result = new ArrayList<ImportTask>(featureTypes.size());
        for (SimpleFeatureType featureType : featureTypes) {
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
            resource.setSRS(KML_SRS);
            resource.setNativeCRS(KML_CRS);
            resource.setNativeBoundingBox(EMPTY_BOUNDS);
            resource.setLatLonBoundingBox(EMPTY_BOUNDS);
            resource.getMetadata().put("recalculate-bounds", Boolean.TRUE);

            Map<Object, Object> userData = featureType.getUserData();
            if (userData.containsKey("schemanames")) {
                MetadataMap metadata = resource.getMetadata();
                metadata.put("importschemanames", (Serializable) userData.get("schemanames"));
            }

            ImportTask task = new ImportTask(data);
            task.setLayer(layer);
            task.setFeatureType(featureType);
            result.add(task);
        }
        return Collections.unmodifiableList(result);
    }

    private String typeNameFromFile(File file) {
        return FilenameUtils.getBaseName(file.getName());
    }
}
