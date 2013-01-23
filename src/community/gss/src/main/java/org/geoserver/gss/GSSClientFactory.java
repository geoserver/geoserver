/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.net.URL;

/**
 * Builds GSSClients.
 * 
 * @author Andrea Aime - OpenGeo 
 */
public interface GSSClientFactory {

    public GSSClient createClient(URL gssServiceURL, String username, String password);
}
