/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public abstract class CatalogRESTTestSupport extends GeoServerTestSupport {

    protected Catalog catalog;
    protected XpathEngine xp;
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory)
            throws Exception {
        super.populateDataDirectory(dataDirectory);

        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();
        
        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        setUpUsers(props);
        props.store(new FileOutputStream(users), "");
        
        File layers = new File(security, "layers.properties");
        props.put("*.*.r", "*");
        props.put("*.*.w", "*");
        setUpLayerRoles(props);
        props.store(new FileOutputStream(layers), "");
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        catalog = getCatalog();
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        doLogin();
    }

    protected void setUpUsers(Properties props) {
    }

    protected void setUpLayerRoles(Properties properties) {
    }

    protected void doLogin() throws Exception {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        l.add(new GrantedAuthorityImpl("ROLE_ADMINISTRATOR"));
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin","geoserver",l));
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        SecurityContextHolder.clearContext();
    }
}
