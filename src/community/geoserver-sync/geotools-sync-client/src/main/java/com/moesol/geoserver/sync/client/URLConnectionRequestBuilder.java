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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLConnectionRequestBuilder implements RequestBuilder {

	@Override
	public Response post(String urlString, String xmlRequest) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection  conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept-encoding", "gzip");
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setDoOutput(true);
        
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), UTF8.UTF8);
        wr.write(xmlRequest);
        wr.flush();

        return new URLConnectionResponse(conn);
	}

}
