/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client;




import static com.moesol.geoserver.sync.client.Features.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.client.RequestBuilderJUnit;
import com.moesol.geoserver.sync.client.Response;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.filter.Sha1SyncFilterFunction;
import com.moesol.geoserver.sync.format.FeatureCollectionSha1Sync;
import com.moesol.geoserver.sync.json.Sha1SyncJson;

final class SimulatedRequestBuilder implements RequestBuilderJUnit {
    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
	private FeatureCollectionType m_server;
	//private FeatureCollectionResponse m_server;
    private final Sha1SyncFilterFunction m_filter = new Sha1SyncFilterFunction();
	private String m_outputFormat;
	private String m_atts;
	private String m_json;
	private VersionFeatures forceVersion;
	
	static final String POST_TEMPLATE = "<wfs:GetFeature " 
	+ "service=\"WFS\" " 
	+ "version=\"1.1.0\" "
	+ "outputFormat=\"${outputFormat}\" "
	+ "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
	+ "xmlns:ogc=\"http://www.opengis.net/ogc\" "
	+ "xmlns:wfs=\"http://www.opengis.net/wfs\" " + ">\n"
	+  "<wfs:Query typeName=\"cite:Buildings\"> "
	+   "<ogc:Filter>"
	+    "<ogc:PropertyIsEqualTo> "
	+      "<ogc:Function name=\"sha1Sync\"> "
	+       "<ogc:Literal>-all</ogc:Literal> "
	+       "<ogc:Literal>${sha1Sync}</ogc:Literal> "
	+      "</ogc:Function> "
	+      "<ogc:Literal>true</ogc:Literal> "
	+    "</ogc:PropertyIsEqualTo> "
	+   "</ogc:Filter> "
	+  "</wfs:Query> " 
	+ "</wfs:GetFeature>";

	SimulatedRequestBuilder(FeatureCollectionType server) {
		m_server = server;
	}
	public void setServer(FeatureCollectionType server) {
		m_server = server;
	}

	@Override
	public Response post(String url, String xml) throws IOException {
		if (m_outputFormat.equals("GML3")) {
			List<Expression> args = new ArrayList<Expression>();
			args.add(ff.literal(m_atts));
			args.add(ff.literal(m_json));
			m_filter.setParameters(args);
			
			FeatureCollectionType serverResp = applyFilter(m_server, m_filter);
			
			TestResponseFeatureCollection respFeatureCollection = new TestResponseFeatureCollection();
			respFeatureCollection.setFeatureCollection((FeatureCollection<?, ?>)serverResp.getFeature().get(0));
			return respFeatureCollection;
		}
		if (m_outputFormat.equals("SyncChecksum")) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			FeatureCollectionSha1Sync sha1Sync = new FeatureCollectionSha1Sync(output);
			sha1Sync.parseAttributesToInclude(m_atts);
			sha1Sync.parseSha1SyncJson(m_json);
			sha1Sync.write(FeatureCollectionResponse.adapt(m_server));
			
			ByteArrayInputStream bais = new ByteArrayInputStream(output.toByteArray());
			TestResponse response = new TestResponse();
			response.setResponseCode(200);
			response.setResponseMessage("OK");
			response.setResultStream(bais);
			return response;
		}
		throw new IllegalStateException("Unexpected output format: " + m_outputFormat);
	}

	private FeatureCollectionType applyFilter(
			final FeatureCollectionType server,
			final Sha1SyncFilterFunction filter) throws IOException {
		List<Feature> filtered = new ArrayList<Feature>();
		
		@SuppressWarnings("unchecked")
		List<FeatureCollection<?,?>> list = server.getFeature();
		FeatureCollection<?, ?> collection = list.get(0);
		@SuppressWarnings("unchecked")
		FeatureIterator<Feature> iterator = (FeatureIterator<Feature>) collection.features();
		try {
			while (iterator.hasNext()) {
				Feature next = iterator.next();
				Object r = filter.evaluate(next);
				if ("true".equals(r)) {
					filtered.add(next);
				}
			}
		} finally {
			Sha1SyncFilterFunction.clearThreadLocals();
			iterator.close();
		}
		FeatureCollectionType serverResp = make(filtered.toArray(new Feature[filtered.size()]));
		return serverResp;
	}

	@Override
	public void prePost(String outputFormat, String atts, String json) {
		m_outputFormat = outputFormat;
		m_atts = atts;
		m_json = json;
		if (this.forceVersion != null) {
			Gson gson = new Gson();
			Sha1SyncJson sha1SyncJson = gson.fromJson(json, Sha1SyncJson.class);
			sha1SyncJson.version(this.forceVersion.getToken());
			m_json = gson.toJson(sha1SyncJson);
		}
	}
	
	public void forceVersion(VersionFeatures version1) {
		this.forceVersion = version1;
	}
}