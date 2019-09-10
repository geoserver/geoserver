/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;

/**
 * Jetty starter, will run geoserver inside the Jetty web container.<br>
 * Useful for debugging, especially in IDE were you have direct dependencies between the sources of
 * the various modules (such as Eclipse).
 *
 * @author wolf
 */
@SuppressWarnings(
        "deprecation") // deep BouncyCastle API changes, need someone that understands it to replace
// current code
public class Start {
    private static final Logger log =
            org.geotools.util.logging.Logging.getLogger(Start.class.getName());

    public static void main(String[] args) {
        final Server jettyServer = new Server();

        try {
            HttpConfiguration httpConfig = new HttpConfiguration();

            ServerConnector http =
                    new ServerConnector(jettyServer, new HttpConnectionFactory(httpConfig));
            http.setPort(Integer.getInteger("jetty.port", 8080));
            http.setAcceptQueueSize(100);
            http.setIdleTimeout(1000 * 60 * 60);
            http.setSoLingerTime(-1);

            // Use this to set a limit on the number of threads used to respond requests
            // BoundedThreadPool tp = new BoundedThreadPool();
            // tp.setMinThreads(8);
            // tp.setMaxThreads(8);
            // conn.setThreadPool(tp);

            // SSL host name given ?
            String sslHost = System.getProperty("ssl.hostname");
            ServerConnector https = null;
            if (sslHost != null && sslHost.length() > 0) {
                Security.addProvider(new BouncyCastleProvider());
                SslContextFactory ssl = createSSLContextFactory(sslHost);

                HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
                httpsConfig.addCustomizer(new SecureRequestCustomizer());

                https =
                        new ServerConnector(
                                jettyServer,
                                new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString()),
                                new HttpConnectionFactory(httpsConfig));
                https.setPort(8443);
            }

            jettyServer.setConnectors(
                    https != null ? new Connector[] {http, https} : new Connector[] {http});

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
            Thread stopThread =
                    new Thread() {
                        @Override
                        public void run() {
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(System.in));
                            String line;
                            try {
                                while (true) {
                                    line = reader.readLine();
                                    if ("stop".equals(line)) {
                                        jettyServer.stop();
                                        System.exit(0);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }
                    };
            stopThread.setDaemon(true);
            stopThread.run();

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
                    log.log(
                            Level.SEVERE,
                            "Unable to stop the " + "Jetty server:" + e1.getMessage(),
                            e1);
                }
            }
        }
    }

    private static SslContextFactory createSSLContextFactory(String hostname) {
        String password = "changeit";

        File userHome = new File(System.getProperty("user.home"));
        File geoserverDir = new File(userHome, ".geoserver");
        if (!geoserverDir.exists()) {
            geoserverDir.mkdir();
        }

        File keyStoreFile = new File(geoserverDir, "keystore.jks");
        try {
            assureSelfSignedServerCertificate(hostname, keyStoreFile, password);
        } catch (Exception e) {
            log.log(Level.WARNING, "NO SSL available", e);
            return null;
        }
        SslContextFactory ssl = new SslContextFactory();
        ssl.setKeyStorePath(keyStoreFile.getAbsolutePath());
        ssl.setKeyStorePassword(password);

        File javaHome = new File(System.getProperty("java.home"));
        File cacerts = new File(javaHome, "lib").toPath().resolve("security/cacerts").toFile();

        if (!cacerts.exists()) {
            return null;
        }

        ssl.setTrustStorePath(cacerts.getAbsolutePath());
        ssl.setTrustStorePassword("changeit");

        return ssl;
    }

    private static void assureSelfSignedServerCertificate(
            String hostname, File keyStoreFile, String password) throws Exception {

        KeyStore privateKS = KeyStore.getInstance("JKS");
        if (keyStoreFile.exists()) {
            FileInputStream fis = new FileInputStream(keyStoreFile);
            privateKS.load(fis, password.toCharArray());
            if (keyStoreContainsCertificate(privateKS, hostname)) return;
        } else {
            privateKS.load(null);
        }

        // create a RSA key pair generator using 1024 bits

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair KPair = keyPairGenerator.generateKeyPair();

        // cerate a X509 certifacte generator
        org.bouncycastle.x509.X509V3CertificateGenerator v3CertGen =
                new org.bouncycastle.x509.X509V3CertificateGenerator();

        // set validity to 10 years, issuer and subject are equal --> self singed certificate
        int random = new SecureRandom().nextInt();
        if (random < 0) random *= -1;
        v3CertGen.setSerialNumber(BigInteger.valueOf(random));
        v3CertGen.setIssuerDN(
                new org.bouncycastle.jce.X509Principal(
                        "CN=" + hostname + ", OU=None, O=None L=None, C=None"));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
        v3CertGen.setNotAfter(
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
        v3CertGen.setSubjectDN(
                new org.bouncycastle.jce.X509Principal(
                        "CN=" + hostname + ", OU=None, O=None L=None, C=None"));

        v3CertGen.setPublicKey(KPair.getPublic());
        v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");

        X509Certificate PKCertificate = v3CertGen.generateX509Certificate(KPair.getPrivate());

        // store the certificate containing the public key,this file is needed
        // to import the public key in other key store.
        File certFile = new File(keyStoreFile.getParentFile(), hostname + ".cert");
        FileOutputStream fos = new FileOutputStream(certFile.getAbsoluteFile());
        fos.write(PKCertificate.getEncoded());
        fos.close();

        privateKS.setKeyEntry(
                hostname + ".key",
                KPair.getPrivate(),
                password.toCharArray(),
                new java.security.cert.Certificate[] {PKCertificate});

        privateKS.setCertificateEntry(hostname + ".cert", PKCertificate);

        privateKS.store(new FileOutputStream(keyStoreFile), password.toCharArray());
    }

    private static boolean keyStoreContainsCertificate(KeyStore ks, String hostname)
            throws Exception {
        SubjectDnX509PrincipalExtractor ex = new SubjectDnX509PrincipalExtractor();
        Enumeration<String> e = ks.aliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if (ks.isCertificateEntry(alias)) {
                Certificate c = ks.getCertificate(alias);
                if (c instanceof X509Certificate) {
                    X500Principal p =
                            (X500Principal) ((X509Certificate) c).getSubjectX500Principal();
                    if (p.getName().contains(hostname)) return true;
                }
            }
        }
        return false;
    }
}
