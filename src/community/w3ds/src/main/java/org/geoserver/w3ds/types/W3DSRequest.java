/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geoserver.w3ds.utilities.Format;
import org.geoserver.w3ds.utilities.Operation;

public abstract class W3DSRequest {
	
	private String service;
	private Operation request;
	private String version;
	private String baseUrl;
	private Map<String, String> kpvPrs;
	private List<Format> acceptedFormats;
	
	public W3DSRequest(String service, Operation request, String version,
			String baseUrl, Map<String, String> kpvPrs,
			List<Format> acceptedFormats) {
		super();
		this.service = service;
		this.request = request;
		this.version = version;
		this.baseUrl = baseUrl;
		this.kpvPrs = kpvPrs;
		this.acceptedFormats = acceptedFormats;
	}
	
	public W3DSRequest(Operation request) {
		this.service = "W3DS";
		this.request = request;
		this.version = "0.0.4";
		this.baseUrl = "geoserver/w3ds?";
		this.kpvPrs = Collections.emptyMap();
		this.acceptedFormats = new ArrayList<Format>();
	}

	public List<Format> getAcceptedFormats() {
		return acceptedFormats;
	}

	public void setAcceptedFormats(List<Format> acceptedFormats) {
		this.acceptedFormats = acceptedFormats;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Operation getRequest() {
		return request;
	}

	public void setRequest(Operation request) {
		this.request = request;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Map<String, String> getKpvPrs() {
		return kpvPrs;
	}

	public void setKpvPrs(Map<String, String> kpvPrs) {
		this.kpvPrs = kpvPrs;
	}
	
	public void addAcceptedFormat(Format format) {
		this.acceptedFormats.add(format);
	}
	
	public Format getValidFormat(String format) {
		for(Format f : acceptedFormats) {
			if(f.getMimeType().equalsIgnoreCase(format)) {
				return f;
			}
		}
		return null;
	}

}
