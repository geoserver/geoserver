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
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class GetSceneRequest extends W3DSRequest {
	
	// Mandatory Parameters
	private CoordinateReferenceSystem crs;
	private Envelope bbox;
	private Format format;
	private List<W3DSLayerInfo> layers;
	
	// OptionalParameters
	private Coordinate offset;
	
	// Hacking to have KML (provisory)
	private GeoServer geoServer;
	
	public GeoServer getGeoServer() {
		return geoServer;
	}

	public void setGeoServer(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	public GetSceneRequest(String service, Operation request, String version,
			String baseUrl, Map<String, String> kpvPrs, CoordinateReferenceSystem crs,
			Envelope bbox, Format format, List<W3DSLayerInfo> layers,
			Coordinate offset) {
		super(service, request, version, baseUrl, kpvPrs, new ArrayList<Format>());
		this.crs = crs;
		this.bbox = bbox;
		this.format = format;
		this.layers = layers;
		this.offset = offset;
		this.addAcceptedFormat(Format.X3D);
		this.addAcceptedFormat(Format.HTML);
		this.addAcceptedFormat(Format.KML);
		this.addAcceptedFormat(Format.XML3D);
		this.addAcceptedFormat(Format.OCTET_STREAM);
	}
	
	public GetSceneRequest() {
		super(Operation.GETSCENE);
		this.crs = null;
		this.bbox = null;
		this.format = null;
		this.layers = null;
		this.offset = null;
		this.addAcceptedFormat(Format.X3D);
		this.addAcceptedFormat(Format.HTML);
		this.addAcceptedFormat(Format.KML);
		this.addAcceptedFormat(Format.XML3D);
		this.addAcceptedFormat(Format.OCTET_STREAM);
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setCrs(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

	public Envelope getBbox() {
		return bbox;
	}

	public void setBbox(Envelope bbox) {
		this.bbox = bbox;
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

	public Coordinate getOffset() {
		return offset;
	}

	public void setOffset(Coordinate offset) {
		this.offset = offset;
	}
	
	public void setMandatoryParameters (String service, Operation request, String version,
			String baseUrl, Map<String, String> kpvPrs, CoordinateReferenceSystem crs,
			Envelope bbox, Format format, List<W3DSLayerInfo> layers) {
		this.setService(service);
		this.setRequest(request);
		this.setVersion(version);
		this.setBaseUrl(baseUrl);
		this.setKpvPrs(kpvPrs);
		this.crs = crs;
		this.bbox = bbox;
		this.format = format;
		this.layers = layers;
	}

}
