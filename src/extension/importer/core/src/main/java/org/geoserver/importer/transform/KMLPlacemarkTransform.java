/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.importer.FeatureDataConverter;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.format.KMLFileFormat;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.Folder;
import org.locationtech.jts.geom.Geometry;

public class KMLPlacemarkTransform extends AbstractTransform implements InlineVectorTransform {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    public SimpleFeatureType convertFeatureType(SimpleFeatureType oldFeatureType) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.add("Geometry", Geometry.class);
        ftb.setDefaultGeometry("Geometry");
        List<AttributeDescriptor> attributeDescriptors = oldFeatureType.getAttributeDescriptors();
        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            String localName = attributeDescriptor.getLocalName();
            if (!"Geometry".equals(localName)) {
                ftb.add(attributeDescriptor);
            }
        }
        ftb.setName(oldFeatureType.getName());
        ftb.setDescription(oldFeatureType.getDescription());
        ftb.setCRS(KMLFileFormat.KML_CRS);
        ftb.setSRS(KMLFileFormat.KML_SRS);
        // remove style attribute for now
        if (oldFeatureType.getDescriptor("Style") != null) {
            ftb.remove("Style");
        }
        ftb.add("Folder", String.class);
        SimpleFeatureType ft = ftb.buildFeatureType();
        return ft;
    }

    public SimpleFeature convertFeature(SimpleFeature old, SimpleFeatureType targetFeatureType) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetFeatureType);
        SimpleFeature newFeature = fb.buildFeature(old.getID());
        FeatureDataConverter.DEFAULT.convert(old, newFeature);
        Map<Object, Object> userData = old.getUserData();
        Object folderObject = userData.get("Folder");
        if (folderObject != null) {
            String serializedFolders = serializeFolders(folderObject);
            newFeature.setAttribute("Folder", serializedFolders);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> untypedExtendedData = (Map<String, String>) userData.get("UntypedExtendedData");
        if (untypedExtendedData != null) {
            for (Entry<String, String> entry : untypedExtendedData.entrySet()) {
                if (targetFeatureType.getDescriptor(entry.getKey()) != null) {
                    newFeature.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        return newFeature;
    }

    private String serializeFolders(Object folderObject) {
        @SuppressWarnings("unchecked")
        List<Folder> folders = (List<Folder>) folderObject;
        List<String> folderNames = new ArrayList<>(folders.size());
        for (Folder folder : folders) {
            String name = folder.getName();
            if (!StringUtils.isEmpty(name)) {
                folderNames.add(name);
            }
        }
        String serializedFolders = StringUtils.join(folderNames.toArray(), " -> ");
        return serializedFolders;
    }

    @Override
    public SimpleFeatureType apply(ImportTask task, DataStore dataStore, SimpleFeatureType featureType)
            throws Exception {
        return convertFeatureType(featureType);
    }

    @Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        SimpleFeatureType targetFeatureType = feature.getFeatureType();
        SimpleFeature newFeature = convertFeature(oldFeature, targetFeatureType);
        feature.setAttributes(newFeature.getAttributes());
        return feature;
    }

    @Override
    public String toString() {
        return "KMLPlacemarkTransform{}";
    }
}
