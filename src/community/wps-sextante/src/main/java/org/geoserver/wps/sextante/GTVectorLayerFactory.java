package org.geoserver.wps.sextante;

import es.unex.sextante.dataObjects.IVectorLayer;

public interface GTVectorLayerFactory {

	public abstract IVectorLayer create(String sName, int iShapeType, Class<?>[] sFields,
			String[] fields, String filename, Object crs);

}