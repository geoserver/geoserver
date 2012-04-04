/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.cas;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.data.test.LiveData;
import org.geotools.util.logging.Logging;

/**
 * Extends LiveData to deal with a CAS server (central authentication server)
 * @author christian
 * 
 */
public class LiveCasData extends LiveData {
    private static final Logger LOGGER = Logging.getLogger(LiveCasData.class);
    private static final String CAS_SERVER_PROPERTY = "serverurl";
    private static final String CAS_SERVICE_PROPERTY = "service";
    private static final String CAS_PROXYCALLBACK_PROPERTY = "proxycallback";

    /**
     * The property file containing the token -> value pairs used to get
     * a CAS server Url
     * 
     * @return
     */
    protected File fixture;
    protected URL serverURL, serviceURL,loginURL, proxyCallbackURL;

    /**
     * List of file paths (relative to the source data directory) that will be
     * subjected to token filtering. By default only <code>catalog.xml</code>
     * will be filtered.
     */


    public URL getServerURL() {
        return serverURL;
    }

    public URL getLoginURL() {
        return loginURL;
    }

    public void setLoginURL(URL loginURL) {
        this.loginURL = loginURL;
    }

    public URL getServiceURL() {
        return serviceURL;
    }

    public URL getProxyCallbackURL() {
        return proxyCallbackURL;
    }

    public void setProxyCallbackURL(URL proxyCallbackURL) {
        this.proxyCallbackURL = proxyCallbackURL;
    }



    /**
     * constant fixture id
     */
    protected String fixtureId="cas";

    public LiveCasData(File dataDirSourceDirectory ) {
        super(dataDirSourceDirectory);
        this.fixture = lookupFixture(fixtureId);
    }

    /**
     * Looks up the fixture file in the home directory provided that the 
     * @param fixtureId
     * @return
     */
    private File lookupFixture(String fixtureId) {
        // first of all, make sure the fixture was not disabled using a system
        // variable
        final String property = System.getProperty("gs." + fixtureId);
        if (property != null && "false".equals(property.toLowerCase())) {
            return null;
        }

        // then look in the user home directory
        File base = new File(System.getProperty("user.home"), ".geoserver");
        // create the hidden folder, this is handy especially on windows where
        // a user cannot create a directory starting with . from the UI 
        // (works only from the command line)
        if(!base.exists())
            base.mkdir();
        File fixtureFile = new File(base, fixtureId + ".properties");
        if (!fixtureFile.exists()) {
            final String warning = "Disabling test based on fixture " + fixtureId + " since the file "
                    + fixtureFile + " could not be found";
            disableTest(warning);
            return null;
        }
                
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(fixtureFile));
            String tmp = props.getProperty(CAS_SERVER_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            serverURL=new URL(tmp);
            loginURL=new URL(tmp+"/login");
            
            tmp = props.getProperty(CAS_SERVICE_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            serviceURL=new URL(tmp);
            
            tmp = props.getProperty(CAS_PROXYCALLBACK_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            proxyCallbackURL=new URL(tmp);

            
        } catch (Exception e) {
            disableTest("Error in fixture file: "+e.getMessage());
            return null;
        }
                
        // check connection
        try {            
            HttpURLConnection huc =  (HttpURLConnection)  loginURL.openConnection(); 
            huc.setRequestMethod("GET"); 
            huc.connect(); 
            if (huc.getResponseCode()!=HttpServletResponse.SC_OK) {
                disableTest("Cannot connect to "+loginURL.toString());
                return null;
            }
        } catch (Exception ex) {
            disableTest("problem with cas connection: "+ex.getMessage());
            return null;            
        }
        
        return fixtureFile;
    }

    public boolean isTestDataAvailable() {
        return fixture != null;
    }

    @Override
    public void setUp() throws Exception {
        // if the test was disabled we don't need to run the setup
        if (fixture == null)
            return;

        super.setUp();
    }

    
    

    /**
     * Permanently disable this test logging the specificed warning message (the reason
     * why the test is being disabled)
     * @param warning
     */
    private void disableTest(final String warning) {
        LOGGER.warning(warning);
        fixture = null;
        System.setProperty("gs." + fixtureId, "false");
    }

}
