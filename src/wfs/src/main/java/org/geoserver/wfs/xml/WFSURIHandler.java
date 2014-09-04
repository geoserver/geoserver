/* 
 * Copyright (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.DescribeFeatureType;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfo.Version;
import org.geoserver.wfs.kvp.DescribeFeatureTypeKvpRequestReader;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.xml.v1_1_0.XmlSchemaEncoder;
import org.geotools.util.logging.Logging;

/**
 * URI handler that handles reflective references back to the server to avoid processing them in 
 * a separate request.
 */
public class WFSURIHandler extends URIHandlerImpl {

    static final Logger LOGGER = Logging.getLogger(WFSURIHandler.class);

    static final Boolean DISABLED;
    static {
        //check for disabled flag
        DISABLED = Boolean.getBoolean(WFSURIHandler.class.getName()+".disabled"); 
    }

    static final List<InetAddress> ADDRESSES = new ArrayList<InetAddress>();
    static {
        if (!DISABLED) {
            // in order to determine if a request is reflective we need to know what all the 
            // addresses and hostnames that we are addressable on. Since hostnames are expensive we 
            // do it once and cache the results
            Enumeration<NetworkInterface> e = null;
            try {
                e = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException ex) {
                LOGGER.log(Level.WARNING, "Unable to determine network interface info", ex);
            } 
            while(e != null && e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                Enumeration<InetAddress> f = ni.getInetAddresses();
                while(f.hasMoreElements()) {
                    InetAddress add = f.nextElement();
    
                    //do the hostname lookup, these are cached after the first call so we only pay the
                    // price now
                    add.getHostName();
                    ADDRESSES.add(add);
                }
            }
        }
    }

    GeoServer geoServer;
   
    public WFSURIHandler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public boolean canHandle(URI uri) {
        if (DISABLED) return false;
        
        //check if this uri is a reflective one
        if (uriIsReflective(uri)) {
            // it is, check the query string to determine if it is a DescribeFeatureType request
            String q = uri.query();
            if (q != null && !"".equals(q.trim())) {
                KvpMap kv = parseQueryString(q);

                if ("DescribeFeatureType".equalsIgnoreCase((String)kv.get("REQUEST")) || 
                        (uri.path().endsWith("DescribeFeatureType"))) {
                   return true;
                }
            }
        }
        return false;
    }

    private KvpMap parseQueryString(String q) {
        return KvpUtils.normalize(KvpUtils.parseQueryString("?" + q));
    }

    private boolean uriIsReflective(URI uri) {
        //TODO: this doesn't take into account the port... or even a different geoserver running 
        // on the same port but in a different application, regardless in that situation the handling
        // of the DFT request will likely fail, falling back to default behavior

        //first check the proxy uri if there is one
        String proxyBaseUrl = geoServer.getGlobal().getProxyBaseUrl(); 
        if (proxyBaseUrl != null) {
            try {
                URI proxyBaseUri = URI.createURI(proxyBaseUrl);
                if(uri.host().equals(proxyBaseUri.host())) {
                    return true;
                }
            }
            catch(IllegalArgumentException e) {
                LOGGER.fine("Unable to parse proxy base url to a uri: " + proxyBaseUrl);
            }
        }
        
        //check the network interfaces to see if the host matches
        for (InetAddress add : ADDRESSES) {
            if (uri.host().equals(add.getHostAddress()) || uri.host().equals(add.getHostName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
        Catalog catalog = geoServer.getCatalog();
        try {
            KvpMap kv = parseQueryString(uri.query());

            //dispatch the correct describe feature type reader
            WFSInfo.Version ver = WFSInfo.Version.negotiate((String)kv.get("VERSION"));
            if(ver == null) {
                ver = WFSInfo.Version.latest();
            }
            DescribeFeatureTypeKvpRequestReader dftReqReader = null;
            switch(ver) {
            case V_10:
            case V_11:
                dftReqReader = new DescribeFeatureTypeKvpRequestReader(catalog);
                break;
            default:
                dftReqReader = 
                    new org.geoserver.wfs.kvp.v2_0.DescribeFeatureTypeKvpRequestReader(catalog);
            }

            //parse the key value pairs
            KvpMap parsed = new KvpMap(kv);
            KvpUtils.parse(parsed);

            //create/read the request object
            DescribeFeatureTypeRequest request = DescribeFeatureTypeRequest.adapt(
                dftReqReader.read(dftReqReader.createRequest(), parsed, kv));

            //set the base url
            //TODO: should this be run through the url mangler? not sure since the uri should 
            // already be "proxified"
            request.setBaseUrl(uri.scheme() + "://" + uri.host() + ":" + uri.port() + uri.path());

            //dispatch the dft operation
            DescribeFeatureType dft = 
                new DescribeFeatureType(geoServer.getService(WFSInfo.class), catalog);
            FeatureTypeInfo[] featureTypes = dft.run(request);

            //generate the response
            XmlSchemaEncoder schemaEncoder = null;
            switch(ver) {
            case V_10:
                schemaEncoder = new XmlSchemaEncoder.V10(geoServer);
                break;
            case V_11:
                schemaEncoder = new XmlSchemaEncoder.V11(geoServer);
                break;
            case V_20:
            default:
                schemaEncoder = new XmlSchemaEncoder.V20(geoServer);
            }

            //build a "dummy" operation descriptor and call the encoder 
            Operation op = new Operation("DescribeFeatureType", new Service("WFS",null,null,null), 
                null, new Object[]{request.getAdaptee()});
            ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
            schemaEncoder.write(featureTypes, bout, op);

            return new ByteArrayInputStream(bout.toByteArray());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to handle DescribeFeatureType uri: " + uri, e);
        } 

        //fall back on regular behaviour
        return super.createInputStream(uri, options);
    }

}
