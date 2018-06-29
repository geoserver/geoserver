/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import org.locationtech.jts.geom.Geometry;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.dataObjects.vectorFilters.IVectorLayerFilter;
import es.unex.sextante.outputs.IOutputChannel;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.util.List;

/**
 * A class to test the StreamOutputChannel class. It does nothing but streaming text descriptions of
 * added features to a print stream
 *
 * @author volaya
 */
public class StreamOutputLayer implements IVectorLayer {

    PrintStream m_Stream;

    /**
     * Constructs this "layer", setting a given output stream as the current one
     *
     * @param stream the print stream to use. When adding a new feature, it will not be stored
     *     anywhere, but just printed to this print stream as a WKT string corresponding to the
     *     geometry and the text representation of each value in the table record
     */
    public StreamOutputLayer(final PrintStream stream) {

        m_Stream = stream;
    }

    public void addFeature(final IFeature feature) {

        addFeature(feature.getGeometry(), feature.getRecord().getValues());
    }

    public void addFeature(final Geometry geom, final Object[] values) {

        m_Stream.println(geom.toText());
        for (int i = 0; i < values.length; i++) {
            m_Stream.println(values[i].toString());
        }
    }

    /**
     * ****************************************************************************************************************************
     * All methods from here are dummy ones and are not supposed to be used
     * ***************************************************************************************************************************
     */
    public int getFieldCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getFieldIndexByName(final String arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getFieldName(final int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getFieldNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public Class getFieldType(final int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Class[] getFieldTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getShapeType() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getShapesCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public IFeatureIterator iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getCRS() {
        // TODO Auto-generated method stub
        return null;
    }

    public Rectangle2D getFullExtent() {
        // TODO Auto-generated method stub
        return null;
    }

    public void close() {
        // TODO Auto-generated method stub
    }

    public Object getBaseDataObject() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public void open() {
        // TODO Auto-generated method stub
    }

    public void postProcess() throws Exception {
        // TODO Auto-generated method stub
    }

    public void setName(final String arg0) {
        // TODO Auto-generated method stub
    }

    public void addFilter(final IVectorLayerFilter filter) {
        // TODO Auto-generated method stub

    }

    public void removeFilters() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canBeEdited() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<IVectorLayerFilter> getFilters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void free() {
        // TODO Auto-generated method stub

    }

    @Override
    public IOutputChannel getOutputChannel() {
        // TODO Auto-generated method stub
        return null;
    }
}
