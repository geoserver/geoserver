/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import java.io.File;

import javax.swing.JDialog;

import org.geotools.referencing.crs.DefaultGeographicCRS;

import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.gui.core.DefaultTaskMonitor;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import es.unex.sextante.rasterWrappers.GridExtent;

public class GTOutputFactory extends OutputFactory {
	
	@Override
	public IVectorLayer getNewVectorLayer(String sName, int iShapeType,
			Class[] types, String[] sFields, IOutputChannel channel, Object crs)
			throws UnsupportedOutputChannelException {

		if (channel instanceof FileOutputChannel) {
			String sFilename = ((FileOutputChannel) channel).getFilename();
			createBaseDir(sFilename);
			sName = sName.replaceAll(" ", "_");
			IVectorLayer vectorLayer = new ShpLayerFactory().create(sName,
					iShapeType, types, sFields, sFilename, crs);
			return vectorLayer;
		} else {
			throw new UnsupportedOutputChannelException();
		}

	}
	
	@Override
	public IVectorLayer getNewVectorLayer(String sName, int iShapeType,
			Class[] types, String[] sFields, IOutputChannel channel,
			Object crs, int[] fieldSize)
			throws UnsupportedOutputChannelException {
		return getNewVectorLayer(sName, iShapeType, types, sFields, channel, crs);
	}

	public IRasterLayer getNewRasterLayer(String sName, int iDataType,
			GridExtent extent, int iBands, IOutputChannel channel, Object crs)
			throws UnsupportedOutputChannelException {

		if (channel instanceof FileOutputChannel) {
			String sFilename = ((FileOutputChannel) channel).getFilename();
			createBaseDir(sFilename);
			GTRasterLayer layer = new GTRasterLayer();
			layer.create(sName, sFilename, extent, iDataType, iBands, crs);
			return layer;
		} else {
			throw new UnsupportedOutputChannelException();
		}

	}

	public ITable getNewTable(String sName, Class types[], String[] sFields,
			IOutputChannel channel) throws UnsupportedOutputChannelException {
		throw new UnsupportedOutputChannelException();
	}

	protected void createBaseDir(String fileName) {
		// creates the base dir if it does not exist
		File file = new File(fileName);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
	}

	protected String getTempFolder() {

		return System.getProperty("java.io.tmpdir");

	}

	public String[] getRasterLayerOutputExtensions() {

		return new String[] { "tif", "asc" };

	}

	public String[] getVectorLayerOutputExtensions() {

		return new String[] { "shp" };

	}

	public String[] getTableOutputExtensions() {
		return new String[] {};
	}

	public DefaultTaskMonitor getTaskMonitor(String sTitle,
			boolean bDeterminate, JDialog parent) {

		return new DefaultTaskMonitor(sTitle, bDeterminate, parent);

	}

	public Object getDefaultCRS() {
		return DefaultGeographicCRS.WGS84;

	}

}
