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

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;


/**
 * Jetty starter, will run geoserver inside the Jetty web container.<br>
 * Useful for debugging, especially in IDE were you have direct dependencies
 * between the sources of the various modules (such as Eclipse).
 *
 * @author wolf
 *
 */
public class Start {
    private static final Logger log = org.geotools.util.logging.Logging.getLogger(Start.class.getName());

    public static void main(String[] args) {
        final Server jettyServer = new Server();

        try {
            SocketConnector conn = new SocketConnector();
            String portVariable = System.getProperty("jetty.port");
            int port = parsePort(portVariable);
            if(port <= 0)
            	port = 8080;
            conn.setPort(port);
            conn.setAcceptQueueSize(100);
            conn.setMaxIdleTime(1000 * 60 * 60);
            conn.setSoLingerTime(-1);
            
            // Use this to set a limit on the number of threads used to respond requests
            // BoundedThreadPool tp = new BoundedThreadPool();
            // tp.setMinThreads(8);
            // tp.setMaxThreads(8);
            // conn.setThreadPool(tp);
            
            // SSL host name given ?
            String sslHost = System.getProperty("ssl.hostname");
            SslSocketConnector sslConn = null;
            if (sslHost!=null && sslHost.length()>0) {   
                Security.addProvider(new BouncyCastleProvider());
                sslConn = getSslSocketConnector(sslHost);
            }
            
            if (sslConn==null) {
                jettyServer.setConnectors(new Connector[] { conn });
            }
            else {
                conn.setConfidentialPort(sslConn.getPort());
                jettyServer.setConnectors(new Connector[] { conn,sslConn });
            }

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
            //this allows to send large SLD's from the styles form
            wah.getServletContext().getContextHandler().setMaxFormContentSize(1024 * 1024 * 2);


            String jettyConfigFile = System.getProperty("jetty.config.file");
            if (jettyConfigFile != null) {
                log.info("Loading Jetty config from file: " + jettyConfigFile);
                (new XmlConfiguration(new FileInputStream(jettyConfigFile))).configure(jettyServer);
            }

           jettyServer.start();

           /*
            * Reads from System.in looking for the string "stop\n" in order to gracefully terminate
            * the jetty server and shut down the JVM. This way we can invoke the shutdown hooks
            * while debugging in eclipse. Can't catch CTRL-C to emulate SIGINT as the eclipse
            * console is not propagating that event
            */
           Thread stopThread = new Thread() {
               @Override
               public void run() {
                   BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
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
                    log.log(Level.SEVERE,
                        "Unable to stop the " + "Jetty server:" + e1.getMessage(), e1);
                }
            }
        }
    }

	private static int parsePort(String portVariable) {
		if(portVariable == null)
			return -1;
	    try {
	    	return Integer.valueOf(portVariable).intValue();
	    } catch(NumberFormatException e) {
	    	return -1;
	    }
	}
	
	private static SslSocketConnector getSslSocketConnector(String hostname) {
	    
	    String  password= "changeit";
	    SslSocketConnector conn = new SslSocketConnector();
	    conn.setPort(8443);
	    File userHome = new File(System.getProperty("user.home"));
	    File geoserverDir = new File(userHome,".geoserver");
	    if (geoserverDir.exists()==false)
	        geoserverDir.mkdir();	    
	    File keyStoreFile = new File(geoserverDir,"keystore.jks");
	    try {
                assureSelfSignedServerCertificate(hostname,keyStoreFile,password);
            } catch (Exception e) {
                log.log(Level.WARNING, "NO SSL available", e);
                return null;                
            }
	    conn.setKeystore(keyStoreFile.getAbsolutePath());
	    conn.setKeyPassword(password);
	    conn.setPassword(password);
	    File javaHome = new File(System.getProperty("java.home"));
	    File cacerts = new File(javaHome,"lib");
	    cacerts =  new File(cacerts,"security");
	    cacerts =  new File(cacerts,"cacerts");
	    
            if (cacerts.exists()== false) 
                return null;
	    conn.setTruststore(cacerts.getAbsolutePath());
	    conn.setTrustPassword("changeit");
	    return conn;
	}
	
	private static void assureSelfSignedServerCertificate(String hostname, File keyStoreFile, String password) throws Exception {
	    
	    
	    KeyStore privateKS = KeyStore.getInstance("JKS");
	    if (keyStoreFile.exists()) {	              
	        FileInputStream fis = new FileInputStream(keyStoreFile);  
	        privateKS.load(fis, password.toCharArray());  	       
	        if (keyStoreContainsCertificate(privateKS,  hostname)) 
	            return;
	    } else {
	        privateKS.load(null);
	    }
	    
	    // create a RSA key pair generator using 1024 bits
	    
	    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");  
	    keyPairGenerator.initialize(1024);  
	    KeyPair KPair = keyPairGenerator.generateKeyPair();  
	    
	    // cerate a X509 certifacte generator
	    X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();  
	    
	    // set validity to 10 years, issuer and subject are equal --> self singed certificate
	    int random = new SecureRandom().nextInt();
	    if (random < 0 ) random*=-1;
	    v3CertGen.setSerialNumber(BigInteger.valueOf(random));  
            v3CertGen.setIssuerDN(new X509Principal("CN=" + hostname + ", OU=None, O=None L=None, C=None"));  
            v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));  
            v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)));  
            v3CertGen.setSubjectDN(new X509Principal("CN=" + hostname + ", OU=None, O=None L=None, C=None"));
                        
            v3CertGen.setPublicKey(KPair.getPublic());  
            v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");   
            
            X509Certificate PKCertificate = v3CertGen.generateX509Certificate(KPair.getPrivate());
            
            // store the certificate containing the public key,this file is needed
            // to import the public key in other key store. 
            File certFile = new File(keyStoreFile.getParentFile(),hostname+".cert");
            FileOutputStream fos = new FileOutputStream(certFile.getAbsoluteFile());  
            fos.write(PKCertificate.getEncoded());  
            fos.close(); 
            
            
            privateKS.setKeyEntry(hostname+".key", KPair.getPrivate(),  
                    password.toCharArray(),  
                    new java.security.cert.Certificate[]{PKCertificate});
            
            privateKS.setCertificateEntry(hostname+".cert",PKCertificate); 
                                             
            privateKS.store( new FileOutputStream(keyStoreFile), password.toCharArray());  
      }
	
      private static boolean keyStoreContainsCertificate(KeyStore ks, String hostname) throws Exception{
          SubjectDnX509PrincipalExtractor ex = new SubjectDnX509PrincipalExtractor();
          Enumeration<String> e = ks.aliases();
          while (e.hasMoreElements()) {
              String alias = e.nextElement();
              if (ks.isCertificateEntry(alias)) {
                  Certificate c =  ks.getCertificate(alias);
                  if (c instanceof X509Certificate) {                       
                      X500Principal p = (X500Principal)  ((X509Certificate) c).getSubjectX500Principal();
                      if (p.getName().contains(hostname)) return true;
                  }
              }
          }
          return false;
      }
}
