/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.gui.core.DefaultTaskMonitor;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import es.unex.sextante.outputs.StreamOutputChannel;
import java.io.File;
import javax.swing.JDialog;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class GTOutputFactory extends OutputFactory {

    @Override
    public IVectorLayer getNewVectorLayer(
            final String sName,
            final int iShapeType,
            final Class[] types,
            final String[] sFields,
            final IOutputChannel channel,
            final Object crs,
            final int[] fieldSize)
            throws UnsupportedOutputChannelException {

        return getNewVectorLayer(sName, iShapeType, types, sFields, channel, crs);
    }

    @Override
    public IVectorLayer getNewVectorLayer(
            final String sName,
            final int iShapeType,
            final Class[] types,
            final String[] sFields,
            final IOutputChannel channel,
            final Object crs)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTVectorLayer vectorLayer = new GTVectorLayer();
            vectorLayer.create(sName, iShapeType, types, sFields, sFilename, crs);
            return vectorLayer;
        } else if (channel instanceof StreamOutputChannel) {
            return new StreamOutputLayer(((StreamOutputChannel) channel).getStream());
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    @Override
    public IRasterLayer getNewRasterLayer(
            final String sName,
            final int iDataType,
            final AnalysisExtent extent,
            final int iBands,
            final IOutputChannel channel,
            final Object crs)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTRasterLayer layer = new GTRasterLayer();
            layer.create(
                    sName, sFilename, extent, iDataType, iBands, crs, this.getDefaultNoDataValue());
            return layer;
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    @Override
    public ITable getNewTable(
            final String sName,
            final Class types[],
            final String[] sFields,
            final IOutputChannel channel)
            throws UnsupportedOutputChannelException {

        if (channel instanceof FileOutputChannel) {
            final String sFilename = ((FileOutputChannel) channel).getFilename();
            createBaseDir(sFilename);
            final GTTable table = new GTTable();
            table.create(sName, sFilename, types, sFields);
            return table;
        } else {
            throw new UnsupportedOutputChannelException();
        }
    }

    protected void createBaseDir(final String fileName) {
        // creates the base dir if it does not exist
        final File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    @Override
    public String getTempFolder() {

        return System.getProperty("java.io.tmpdir");
    }

    @Override
    public String[] getRasterLayerOutputExtensions() {

        return new String[] {"tif", "asc"};
    }

    @Override
    public String[] getVectorLayerOutputExtensions() {

        return new String[] {"shp"};
    }

    @Override
    public String[] getTableOutputExtensions() {

        return new String[] {"dbf"};
    }

    @Override
    public DefaultTaskMonitor getTaskMonitor(
            final String sTitle, final boolean bDeterminate, final JDialog parent) {

        return new DefaultTaskMonitor(sTitle, bDeterminate, parent);
    }

    @Override
    public Object getDefaultCRS() {

        return DefaultGeographicCRS.WGS84;
    }
}
