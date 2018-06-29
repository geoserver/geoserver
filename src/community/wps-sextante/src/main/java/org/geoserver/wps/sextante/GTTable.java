/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import org.locationtech.jts.geom.Geometry;
import es.unex.sextante.dataObjects.AbstractTable;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileException;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

public class GTTable extends AbstractTable {

    private String m_sName;
    private String m_sFilename;
    private int m_iCount;
    private FeatureSource<SimpleFeatureType, SimpleFeature> m_BaseDataObject;

    public void create(final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {

        try {
            m_BaseDataObject = featureSource;
            m_iCount = featureSource.getFeatures().size();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void addRecord(final Object[] values) {

        if (getFeatureSource() instanceof FeatureStore) {
            try {
                final FeatureStore<SimpleFeatureType, SimpleFeature> store =
                        ((FeatureStore<SimpleFeatureType, SimpleFeature>) getFeatureSource());
                final List<Object> attributes = new ArrayList<Object>();
                attributes.addAll(Arrays.asList(values));
                final SimpleFeatureType ft = store.getSchema();
                final DefaultFeatureCollection collection = new DefaultFeatureCollection();

                final SimpleFeature feature =
                        SimpleFeatureBuilder.build(
                                ft, attributes, SimpleFeatureBuilder.createDefaultFeatureId());
                collection.add(feature);
                store.addFeatures(collection);
                m_iCount++;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void create(
            final String sName,
            final String sFilename,
            final Class<?>[] fields,
            final String[] sFields) {

        try {
            m_sFilename = sFilename;
            m_sName = sName;
            final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName(sName);

            final AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
            for (int i = 0; i < sFields.length; i++) {
                final AttributeType type = attBuilder.binding(fields[0]).buildType();
                final AttributeDescriptor descriptor = attBuilder.buildDescriptor(sFields[i], type);
                builder.add(descriptor);
            }
            final DataStore mds = new MemoryDataStore();
            mds.createSchema(builder.buildFeatureType());

            create(mds.getFeatureSource(mds.getTypeNames()[0]));

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public IRecordsetIterator iterator() {

        if (m_BaseDataObject != null) {
            try {
                final FeatureCollection<SimpleFeatureType, SimpleFeature> features =
                        getFeatureSource().getFeatures();
                return new GTRecordsetIterator(features);
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
        return m_BaseDataObject;
    }

    public String getFieldName(final int i) {

        if (m_BaseDataObject != null) {
            try {
                final SimpleFeatureType ft = getFeatureSource().getSchema();
                final AttributeType at = ft.getType(i);
                return at.getName().getLocalPart();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public Class<?> getFieldType(final int i) {

        if (m_BaseDataObject != null) {
            try {
                final SimpleFeatureType ft = getFeatureSource().getSchema();
                final AttributeType at = ft.getType(i);
                return at.getBinding();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public int getFieldCount() {

        if (m_BaseDataObject != null) {
            try {
                final SimpleFeatureType ft = getFeatureSource().getSchema();
                return ft.getAttributeCount();
            } catch (final Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        return 0;
    }

    public long getRecordCount() {

        return m_iCount;
    }

    public void close() {}

    public String getName() {

        return m_sName;
    }

    public void open() {}

    public void postProcess() {}

    protected DbaseFileHeader createDbaseHeader() throws IOException, DbaseFileException {

        final SimpleFeatureType featureType = getFeatureSource().getSchema();
        final DbaseFileHeader header = new DbaseFileHeader();
        for (int i = 0, ii = featureType.getAttributeCount(); i < ii; i++) {

            final AttributeType type = featureType.getType(i);

            final Class colType = type.getBinding();
            final String colName = type.getName().getLocalPart();
            final int fieldLen = 255;

            if ((colType == Integer.class) || (colType == Short.class) || (colType == Byte.class)) {
                header.addColumn(colName, 'N', Math.min(fieldLen, 16), 0);
            } else if ((colType == Double.class)
                    || (colType == Float.class)
                    || (colType == Number.class)) {
                final int l = Math.min(fieldLen, 33);
                header.addColumn(colName, 'N', l, l / 2);
            } else if (java.util.Date.class.isAssignableFrom(colType)) {
                header.addColumn(colName, 'D', fieldLen, 0);
            } else if (colType == Boolean.class) {
                header.addColumn(colName, 'L', 1, 0);
            } else if (CharSequence.class.isAssignableFrom(colType)) {
                header.addColumn(colName, 'C', Math.min(254, fieldLen), 0);
            } else if (Geometry.class.isAssignableFrom(colType)) {
                continue;
            } else {
                throw new IOException("Unable to write : " + colType.getName());
            }
        }
        return header;
    }

    public IOutputChannel getOutputChannel() {

        return new FileOutputChannel(m_sFilename);
    }

    public void setName(final String name) {

        m_sName = name;
    }

    @Override
    public void free() {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getBaseDataObject() {

        return m_BaseDataObject;
    }
}
