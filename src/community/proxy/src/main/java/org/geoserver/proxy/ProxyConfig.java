/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxy;

import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;

import com.thoughtworks.xstream.XStream;

/**
 * This class holds the the configuration for the Proxy server module during runtime. It is also
 * serialized as XML to persistently store settings.
 * 
 * @author Alan Gerber <agerber@openplans.org>
 */
//vvv everybody else is doing this
@SuppressWarnings("serial")
public class ProxyConfig implements java.io.Serializable{
    /*
     * Sets the mode of the proxy server: -HOSTNAMEORMIMETYPE means a request must match have
     * matches on the hostname AND MIMEType whitelists -HOSTNAMEANDMIMETYPE means a request must
     * match have matches on the hostname OR MIMEType whitelists -HOSTNAME means a request must
     * match have a match on the hostname whitelist alone -MIMETYPE means a request must match have
     * a match on the MIMEType whitelist alone
     */
    public enum Mode {
        HOSTNAMEORMIMETYPE ("Hostname OR MIMEType"),
        HOSTNAMEANDMIMETYPE ("Hostname AND MIMEType"),
        HOSTNAME ("Hostname only"),
        MIMETYPE ("MIMEType only");
        
        public final String modeName;
        Mode(String modeName){
            this.modeName = modeName;
        }
        
        public static List<String> modeNames() {
            List<String> modeNames = new ArrayList<String>();
            for (Mode mode : Mode.values())
            {
                modeNames.add(mode.modeName);
            }
            return modeNames;
        }
    };

    public Mode mode;

    /* A list of regular expressions describing hostnames the proxy is permitted to forward to */
    public LinkedHashSet<String> hostnameWhitelist;

    /* A list of regular expressions describing MIMETypes the proxy is permitted to forward */
    public LinkedHashSet<String> mimetypeWhitelist;

    /*The default proxy configuration allows all requests to localhost and all OGC mimetypes*/
    private static final ProxyConfig DEFAULT;
    static {
        DEFAULT = new ProxyConfig();
        DEFAULT.mode = Mode.HOSTNAMEORMIMETYPE;
        DEFAULT.hostnameWhitelist = new LinkedHashSet<String>();
        DEFAULT.mimetypeWhitelist = new LinkedHashSet<String>(Arrays.asList(
                "application/xml", "text/xml",
                "application/vnd.ogc.se_xml",           // OGC Service Exception 
                "application/vnd.ogc.se+xml",           // OGC Service Exception
                "application/vnd.ogc.success+xml",      // OGC Success (SLD Put)
                "application/vnd.ogc.wms_xml",          // WMS Capabilities
                "application/vnd.ogc.context+xml",      // WMC
                "application/vnd.ogc.gml",              // GML
                "application/vnd.ogc.sld+xml",          // SLD
                "application/vnd.google-earth.kml+xml"  // KML;
        ));
    }

    private static final Logger LOG = org.geotools.util.logging.Logging
            .getLogger("org.geoserver.proxy");

    /* this is pretty unappealingly hackish */
    public static ProxyConfig loadConfFromDisk() {
        ProxyConfig retval;
        Resource.Lock lock = null;
        try {
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource configFile = loader.get( "proxy/proxy.xml" );
            lock = configFile.lock();
            
            InputStream proxyConfStream = configFile.in();
            XStream xs = new XStream();
            //Take the read lock, then read the file
            retval = (ProxyConfig) (xs.fromXML(proxyConfStream));
        } catch (Exception e) {
            LOG.warning("Failed to open configuration for Proxy module. Using default. Exception:"
                    + e.toString());
            //writeConfigToDisk(DEFAULT);
            retval = DEFAULT;
        }
        finally {
            if( lock != null ) lock.release();
        }
        return retval;
    }

    public static boolean writeConfigToDisk(ProxyConfig pc) {
        Resource.Lock lock = null;
        try {
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource configFile = loader.get( "proxy/proxy.xml" );
            
            XStream xs = new XStream();
            String xml = xs.toXML(pc);
            FileWriter fw = new FileWriter(configFile.file(), false); // false means overwrite old file
            //Take the write lock on the file & lock it
            lock = configFile.lock();
            fw.write(xml);
            fw.close();
            return true;
        } catch (Exception e) {
            LOG.warning("Failed to save configuration for Proxy module. Exception:"
                    + e.toString());
            return false;
        }
        finally {
            if( lock != null ) lock.release();
        }
    }
    
    /*Output a textual representation of the config
     *@return a String representation of the config
     */
    @Override
    public String toString(){
        StringBuilder stringForm = new StringBuilder(256);
        stringForm.append("Mode: " + this.mode.modeName + "\n");
        stringForm.append("Hostname regex whitelist: \n");
        for (String hostname : this.hostnameWhitelist)
            stringForm.append(hostname + "\n");
        stringForm.append("MIMEType regex whitelist: \n");
        for (String mimetype : this.mimetypeWhitelist)
            stringForm.append(mimetype + "\n");
        stringForm.append(this.mode.modeName + "\n");        
        return stringForm.toString();
    }
}
