/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import es.unex.sextante.dataObjects.AbstractRasterLayer;
import es.unex.sextante.rasterWrappers.GridExtent;

public class GTRasterLayer extends AbstractRasterLayer {

	private CoordinateReferenceSystem m_CRS;
	private String m_sFilename;
	private String m_sName ="";
	private PlanarImage m_image;
	private GridExtent m_LayerExtent;
	private double m_dNoDataValue;

	public void create(String name, String filename, GridExtent ge,
			int dataType, int numBands, Object crs) {

		if (!(crs instanceof CoordinateReferenceSystem)){
			crs = DefaultGeographicCRS.WGS84;
		}

		Raster m_Raster = RasterFactory.createBandedRaster(dataType,
								ge.getNX(), ge.getNY(), numBands, null);

		Envelope envelope = new Envelope2D((CoordinateReferenceSystem)crs,
											ge.getXMin(), ge.getYMin(),
											ge.getWidth(), ge.getHeight());
		GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
		GridCoverage2D gc = factory.create(name, (WritableRaster)m_Raster, envelope,
				null, null, null, null, null);
		m_BaseDataObject = gc;
		m_sName = name;
		m_sFilename = filename;
		m_LayerExtent = ge;
		m_image = (PlanarImage) gc.geophysics(true).getRenderedImage();
		initNoData((GridCoverage2D)m_BaseDataObject);

	}

	private void initNoData(GridCoverage2D gc) {
		Object value = gc.getProperty("GC_NODATA");
		if (value!=null && value instanceof Number) {
			m_dNoDataValue = ((Number) value).doubleValue();
			return;
		}
		else {
			GridSampleDimension[] dimList = gc.getSampleDimensions();
			double[] noDataList;
			for (int i=0; i<dimList.length; i++) {
				noDataList = dimList[i].getNoDataValues(); 
				if (noDataList != null && noDataList.length>0){
					m_dNoDataValue = noDataList[0];
					return;
				}
			}
		}
		// ensure we got a sensible result, because some GridCoverage2D are not able to provide no-data values
		setNoDataValue(Double.NaN);
	}

	public void create(Object obj) {

		if (obj instanceof GridCoverage2D){
			m_BaseDataObject = obj;
			GridCoverage2D gc = ((GridCoverage2D)obj);
			m_CRS = gc.getCoordinateReferenceSystem();
			Envelope2D env = gc.getEnvelope2D();
			m_LayerExtent = new GridExtent();
			m_LayerExtent.setCellSize((env.getMaxX() - env.getMinX())
								/ (double)gc.getRenderedImage().getWidth());
			m_LayerExtent.setXRange(env.getMinX(), env.getMaxX());
			m_LayerExtent.setYRange(env.getMinY(), env.getMaxY());
			m_image = (PlanarImage) gc.geophysics(true).getRenderedImage();
			m_sName = gc.getName().toString();
			initNoData(gc);
		}

	}

	public void fitToGridExtent(GridExtent gridExtent) {

		if (gridExtent != null){
			WritableRaster raster = RasterFactory.createBandedRaster(getDataType(),
									gridExtent.getNX(), gridExtent.getNY(),
									getBandsCount(), null);

			Envelope envelope = new Envelope2D((CoordinateReferenceSystem)m_CRS,
									gridExtent.getXMin(), gridExtent.getYMin(),
									gridExtent.getWidth(), gridExtent.getHeight());
			GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

			this.setWindowExtent(gridExtent);
			// FIXME: should this also use tiles???
			for (int x = 0; x < gridExtent.getNX(); x++) {
				for (int y = 0; y < gridExtent.getNY(); y++) {
					for (int i = 0; i < getBandsCount(); i++) {
						raster.setSample(x, y, i, this.getCellValueAsDouble(x, y, i));
					}
				}

			}
			m_BaseDataObject = factory.create(getName(), (WritableRaster)raster, envelope,
											null, null, null, null, null);

			System.gc();
		}

	}

	public int getBandsCount() {

		if (m_BaseDataObject != null){
			GridCoverage2D gc = (GridCoverage2D) m_BaseDataObject;
			return gc.getNumSampleDimensions();
		}
		else{
			return 0;
		}

	}

	public double getCellValueInLayerCoords(int x, int y, int band) {

		if (m_image != null){
			Raster tile = getTile(x, y);
            if(tile != null) {
			    return tile.getSampleDouble(x, y, band);
			} else {
			    return getNoDataValue();
			}
		} else {
			return getNoDataValue();
		}

	}

	public int getDataType() {

		if (m_image != null){
			return m_image.getTile(0, 0).getDataBuffer().getDataType();
		}
		else{
			return DataBuffer.TYPE_DOUBLE;
		}

	}

	public double getLayerCellSize() {

		if (m_LayerExtent != null){
			return m_LayerExtent.getCellSize();
		}
		else{
			return 0;
		}

	}

	public GridExtent getLayerGridExtent() {

		return m_LayerExtent;

	}

	public double getNoDataValue() {

		return m_dNoDataValue;

	}

	public void setCellValue(int x, int y, int band, double value) {

		if (isInWindow(x, y)){
			Raster raster = getTile(x, y);
			if (raster instanceof WritableRaster){
				((WritableRaster)raster).setSample(x, y, band, value);
			}
		}

	}

	public void setNoDataValue(double noDataValue) {

		m_dNoDataValue = noDataValue;

	}

	public Object getCRS() {

		return m_CRS;

	}

	public Rectangle2D getFullExtent() {

		if (m_BaseDataObject != null){
			GridCoverage2D gc = (GridCoverage2D) m_BaseDataObject;
			return new Envelope2D(gc.getEnvelope());
		}
		else{
			return null;
		}

	}

	public void open() {}

	public void close() {}

	public void postProcess() {

		try{
			AbstractGridCoverageWriter writer;
			if (m_sFilename.endsWith("asc")){
				writer = new ArcGridWriter(new File(m_sFilename));
			}
			else{
				writer = new GeoTiffWriter(new File(m_sFilename));
			}
			GridCoverage2D gc = (GridCoverage2D) m_BaseDataObject;
			writer.write(gc.geophysics(true), null);
			writer.dispose();
		}catch (Exception e){
			e.printStackTrace();
		}

	}

	public String getFilename() {

		return m_sFilename;

	}

	public String getName() {

		return m_sName;

	}

	public void setName(String sName) {

		m_sName = sName;

	}

	private Raster getTile(int x, int y) {
		return m_image.getTile(m_image.XToTileX(x), m_image.YToTileY(y));
		
	}
}
