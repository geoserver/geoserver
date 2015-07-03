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




import java.io.IOException;
import java.io.InputStream;

import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import static org.junit.Assert.*;

import com.moesol.geoserver.sync.client.ResponseFeatureCollection;

public class TestResponseFeatureCollection implements ResponseFeatureCollection {
	private int m_responseCode;
	private String m_responseMessage;
	private InputStream m_resultStream;
	private FeatureCollection<?, ?> m_featureCollection;

	@Override
	public int getResponseCode() throws IOException {
		return m_responseCode;
	}
	public void setResponseCode(int responseCode) {
		m_responseCode = responseCode;
	}

	@Override
	public String getResponseMessage() throws IOException {
		return m_responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		m_responseMessage = responseMessage;
	}

	@Override
	public InputStream getResultStream() throws IOException {
		return m_resultStream;
	}
	public void setResultStream(InputStream resultStream) {
		m_resultStream = resultStream;
	}
	
	@Override
	public FeatureCollection<?, ?> getFeatureCollection() {
		return m_featureCollection;
	}
	public void setFeatureCollection(FeatureCollection<?, ?> featureCollection) {
		m_featureCollection = featureCollection;
	}
	@Override
	public String getContentEncoding() {
		return "text/xml";
	}
	@Test
	public void dummyTest(){
		assertEquals(1, 1);
		
	}

}
