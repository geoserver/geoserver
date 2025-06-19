/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Jetty starter, will run geoserver inside the Jetty web container.<br>
 * Useful for debugging, especially in IDE were you have direct dependencies between the sources of the various modules
 * (such as Eclipse).
 *
 * @author wolf
 */
@SuppressWarnings("deprecation") // deep BouncyCastle API changes, need someone that understands it to replace
// current code
public class Start {
    private static final Logger log = org.geotools.util.logging.Logging.getLogger(Start.class.getName());

    public static void main(String[] args) {
        final Server jettyServer = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        try (ServerConnector http = new ServerConnector(jettyServer, new HttpConnectionFactory(httpConfig));
                ServerConnector https = getHTTPSConnector(jettyServer, httpConfig)) {
            http.setPort(Integer.getInteger("jetty.port", 8080));
            http.setAcceptQueueSize(100);
            http.setIdleTimeout(1000 * 60 * 60);
            http.setSoLingerTime(-1);

            // Use this to set a limit on the number of threads used to respond requests
            // BoundedThreadPool tp = new BoundedThreadPool();
            // tp.setMinThreads(8);
            // tp.setMaxThreads(8);
            // conn.setThreadPool(tp);

            jettyServer.setConnectors(https != null ? new Connector[] {http, https} : new Connector[] {http});

            // uncomment call and customize method to create a JNDI data source
            // addJNDIDataSource();

            /*Constraint constraint = new Constraint();
            constraint.setName(Constraint.__BASIC_AUTH);;
            constraint.setRoles(new String[]{"user","admin","moderator"});
            constraint.setAuthenticate(true);

            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec("/*");

            SecurityHandler sh = new SecurityHandler();
            sh.setUserRealm(new HashUserRealm("MyRealm","/Users/jdeolive/realm.properties"));
            sh.setConstraintMappings(new ConstraintMapping[]{cm});

            WebAppContext wah = new WebAppContext(sh, null, null, null);*/
            WebAppContext wah = new WebAppContext();
            wah.setContextPath("/geoserver");
            wah.setWar("src/main/webapp");

            jettyServer.setHandler(wah);
            wah.setTempDirectory(new File("target/work"));
            // this allows to send large SLD's from the styles form
            wah.getServletContext().getContextHandler().setMaxFormContentSize(1024 * 1024 * 5);
            // this allows to configure hyperspectral images
            wah.getServletContext().getContextHandler().setMaxFormKeys(2000);

            String jettyConfigFile = System.getProperty("jetty.config.file");
            if (jettyConfigFile != null) {
                log.info("Loading Jetty config from file: " + jettyConfigFile);
                (new XmlConfiguration(new FileInputStream(jettyConfigFile))).configure(jettyServer);
            }

            long start = System.currentTimeMillis();
            log.severe("GeoServer starting");

            jettyServer.start();

            long end = System.currentTimeMillis();
            log.severe("GeoServer startup complete in " + (end - start) / 1000. + "s");

            /*
             * Reads from System.in looking for the string "stop\n" in order to gracefully terminate
             * the jetty server and shut down the JVM. This way we can invoke the shutdown hooks
             * while debugging in eclipse. Can't catch CTRL-C to emulate SIGINT as the eclipse
             * console is not propagating that event
             */
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    String line = reader.readLine();
                    if ("stop".equals(line)) {
                        jettyServer.stop();
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // NOPMD
                System.exit(1);
            }

            // use this to test normal stop behaviour, that is, to check stuff that
            // need to be done on container shutdown (and yes, this will make
            // jetty stop just after you started it...)
            // jettyServer.stop();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Could not start the Jetty server: " + e.getMessage(), e);

            if (jettyServer != null) {
                try {
                    jettyServer.stop();
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "Unable to stop the " + "Jetty server:" + e1.getMessage(), e1);
                }
            }
        }
    }

    /**
     * Adds a JNDI data source to the Jetty server. Uncomment call in the main method, and customize the pool parameters
     * and name as needed.
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static void addJNDIDataSource() throws NamingException {
        // Create the JNDI data source
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/mydb");
        dataSource.setUsername("user");
        dataSource.setPassword("pwd");
        dataSource.setMaxActive(20);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(2);
        dataSource.setInitialSize(5);
        dataSource.setAccessToUnderlyingConnectionAllowed(true);

        // Bind the data source to JNDI
        new Resource("java:comp/env/jdbc/myds", dataSource);
    }

    private static ServerConnector getHTTPSConnector(Server jettyServer, HttpConfiguration httpConfig) {
        // SSL host name given ?
        String sslHost = System.getProperty("ssl.hostname");
        ServerConnector https = null;
        if (sslHost != null && !sslHost.isEmpty()) {
            // SSL functionality disabled due to BouncyCastle FIPS compatibility issues
            // TODO: Implement SSL using standard Java security classes
            log.warning("SSL functionality is currently disabled. HTTPS connector will not be created.");
        }
        return https;
    }

    private static SslContextFactory createSSLContextFactory(String hostname) {
        // SSL functionality disabled due to BouncyCastle FIPS compatibility issues
        // TODO: Implement SSL using standard Java security classes
        return null;
    }

    private static void assureSelfSignedServerCertificate(String hostname, File keyStoreFile, String password)
            throws Exception {
        // SSL functionality disabled due to BouncyCastle FIPS compatibility issues
        // TODO: Implement certificate generation using standard Java security classes
        throw new UnsupportedOperationException(
                "SSL certificate generation is currently disabled due to BouncyCastle FIPS compatibility issues");
    }

    private static boolean keyStoreContainsCertificate(KeyStore ks, String hostname) throws Exception {
        Enumeration<String> e = ks.aliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if (ks.isCertificateEntry(alias)) {
                Certificate c = ks.getCertificate(alias);
                if (c instanceof X509Certificate) {
                    X500Principal p = ((X509Certificate) c).getSubjectX500Principal();
                    if (p.getName().contains(hostname)) return true;
                }
            }
        }
        return false;
    }
}
