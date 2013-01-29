/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class GetFeatureInfoRequest extends W3DSRequest {

	// Mandatory Parameters
	private CoordinateReferenceSystem crs;
	private Coordinate coordinate;
	private Format format;
	private List<W3DSLayerInfo> layers;

	// Must be remove
	private GeoServer geoServer;

	public GeoServer getGeoServer() {
		return geoServer;
	}

	public void setGeoServer(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	public GetFeatureInfoRequest(String service, Operation request,
			String version, String baseUrl, Map<String, String> kpvPrs,
			CoordinateReferenceSystem crs, Format format,
			List<W3DSLayerInfo> layers, Coordinate coordinate) {
		super(service, request, version, baseUrl, kpvPrs,
				new ArrayList<Format>());
		this.crs = crs;
		this.format = format;
		this.layers = layers;
		this.coordinate = coordinate;
		this.addAcceptedFormat(Format.HTML);
	}

	public GetFeatureInfoRequest() {
		super(Operation.GETFEATUREINFO);
		this.crs = null;
		this.format = null;
		this.layers = null;
		this.coordinate = null;
		this.addAcceptedFormat(Format.HTML);
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setCrs(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public List<W3DSLayerInfo> getLayers() {
		return layers;
	}

	public void setLayers(List<W3DSLayerInfo> layers) {
		this.layers = layers;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public void setMandatoryParameters(String service, Operation request,
			String version, String baseUrl, Map<String, String> kpvPrs,
			CoordinateReferenceSystem crs, Format format,
			List<W3DSLayerInfo> layers, Coordinate coordinate) {
		this.setService(service);
		this.setRequest(request);
		this.setVersion(version);
		this.setBaseUrl(baseUrl);
		this.setKpvPrs(kpvPrs);
		this.crs = crs;
		this.format = format;
		this.layers = layers;
		this.coordinate = coordinate;
	}

}
