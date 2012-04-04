/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;


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
            
            jettyServer.setConnectors(new Connector[] { conn });

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
}
