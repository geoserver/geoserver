/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.geoserver.catalog.Catalog;
import org.geoserver.gss.xml.GSSConfiguration;
import org.springframework.beans.factory.DisposableBean;

/**
 * Builds http clients for GSS. Keeps a hold on the single commons http client used to connect to
 * all hosts and shuts it down on
 * 
 * @author aaime
 * 
 */
public class HTTPGSSClientFactory implements DisposableBean, GSSClientFactory {

    HttpClient client;

    GSSConfiguration configuration;

    Catalog catalog;

    public HTTPGSSClientFactory(GSSConfiguration configuration, Catalog catalog) {
        this.configuration = configuration;
        this.catalog = catalog;
    }

    HttpClient getClient() {
        if (client == null) {
            client = new HttpClient();
            HttpConnectionManagerParams params = new HttpConnectionManagerParams();
            // setting timeouts (one minute hard coded, TODO: make this configurable)
            params.setSoTimeout(60 * 1000);
            params.setConnectionTimeout(60 * 1000);
            params.setDefaultMaxConnectionsPerHost(1);
            MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
            manager.setParams(params);
            client.setHttpConnectionManager(manager);
        }
        return client;
    }

    public GSSClient createClient(URL gssServiceURL, String username, String password) {
        return new HTTPGSSClient(getClient(), configuration, catalog, gssServiceURL, username,
                password);
    }

    public void destroy() throws Exception {
        if (client != null) {
            ((MultiThreadedHttpConnectionManager) client.getHttpConnectionManager()).shutdownAll();
            client = null;
        }
    }
}
