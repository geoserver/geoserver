/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.geoserver.wps.remote.plugin.server;

import java.io.IOException;
import java.util.List;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;
import org.apache.vysper.xmpp.extension.xep0124.BoshServlet;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Based on original {@link BoshEndpoint}, which uses an old version of jetty-server and
 * jetty-security.
 *
 * <p>Allows HTTP clients to communicate via the BOSH protocol with Vysper.
 *
 * <p>See http://xmpp.org/extensions/xep-0124.html and http://xmpp.org/extensions/xep-0206.html
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @author Alessio Fabiani, GeoSolutions S.A.S. *
 */
public class XMPPBoshEndpoint implements Endpoint {
    protected static final Logger logger = LoggerFactory.getLogger(XMPPBoshEndpoint.class);

    protected ServerRuntimeContext serverRuntimeContext;

    protected int port = 5222;

    protected Server server;

    protected boolean isSSLEnabled;

    protected String sslKeystorePath;

    protected String sslKeystorePassword;

    protected List<String> accessControlAllowOrigin;

    protected String contextPath = "/";

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    /** Setter for the listen port */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Configures the SSL keystore
     *
     * <p>Required if SSL is enabled. Also, setting the keystore password is required.
     *
     * @see #setSSLCertificateKeystorePassword
     * @param keystorePath the path to the Java keystore
     */
    public void setSSLCertificateKeystore(Resource keystorePath) throws IOException {
        sslKeystorePath = keystorePath.getFile().getAbsolutePath();
    }

    /**
     * Configures the SSL keystore
     *
     * <p>Required if SSL is enabled. Also, setting the keystore password is required.
     *
     * @see #setSSLCertificateKeystorePassword
     * @param keystorePath the path to the Java keystore
     */
    public void setSSLCertificateKeystore(String keystorePath) {
        sslKeystorePath = keystorePath;
    }

    /**
     * Configures the SSL keystore password.
     *
     * <p>Required if SSL is enabled. Also, the keystore must be set using {@link
     * #setSSLCertificateKeystore(String)} } The password is used both for accessing the keystore
     * and for recovering the key from the keystore. The unique password is a limitation, you cannot
     * use different passwords for the keystore and for the key.
     *
     * @param password the password used as the keystore password and also used when recovering the
     *     key from the keystore
     */
    public void setSSLCertificateKeystorePassword(String password) {
        sslKeystorePassword = password;
    }

    /**
     * Enables/disables SSL for this endpoint.
     *
     * <p>If SSL is enabled it requires SSL certificate information that can be configured with
     * {@link #setSSLCertificateInfo(String, String)}
     */
    public void setSSLEnabled(boolean value) {
        isSSLEnabled = value;
    }

    /**
     * Get the list of domains allowed to access this endpoint
     *
     * @return The list of allowed domains
     */
    public List<String> getAccessControlAllowOrigin() {
        return accessControlAllowOrigin;
    }

    /**
     * Set the list of domains allowed to access this endpoint
     *
     * @param accessControlAllowOrigin The list of allowed domains
     */
    public void setAccessControlAllowOrigin(List<String> accessControlAllowOrigin) {
        this.accessControlAllowOrigin = accessControlAllowOrigin;
    }

    /**
     * Determines the context URI where the BOSH transport will be accessible. The default is as
     * 'root context' under '/'.
     */
    public void setContextPath(String contextPath) {
        if (contextPath == null) contextPath = "/";
        this.contextPath = contextPath;
    }

    /**
     * create a basic Jetty server including a connector on the configured port override in subclass
     * to create a different kind of setup or to reuse an existing instance
     */
    protected Server createJettyServer() {
        Server server = new Server();
        ServerConnector connector;

        // HTTP Configuration
        // HttpConfiguration is a collection of configuration information
        // appropriate for http and https. The default scheme for http is
        // <code>http</code> of course, as the default for secured http is
        // <code>https</code> but we show setting the scheme to show it can be
        // done. The port for secured communication is also set here.
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(port);
        http_config.setOutputBufferSize(32768);

        if (isSSLEnabled) {
            // SSL Context Factory for HTTPS
            // SSL requires a certificate so we configure a factory for ssl contents
            // with information pointing to what keystore the ssl connection needs
            // to know about. Much more configuration is available the ssl context,
            // including things like choosing the particular certificate out of a
            // keystore to be used.
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(sslKeystorePath);
            sslContextFactory.setKeyManagerPassword(sslKeystorePassword);
            sslContextFactory.setKeyStorePassword(sslKeystorePassword);

            // HTTPS Configuration
            // A new HttpConfiguration object is needed for the next connector and
            // you can pass the old one as an argument to effectively clone the
            // contents. On this HttpConfiguration object we add a
            // SecureRequestCustomizer which is how a new connector is able to
            // resolve the https connection before handing control over to the Jetty
            // Server.
            HttpConfiguration https_config = new HttpConfiguration(http_config);
            SecureRequestCustomizer src = new SecureRequestCustomizer();
            // src.setStsMaxAge(2000);
            // src.setStsIncludeSubDomains(true);
            https_config.addCustomizer(src);

            // HTTPS connector
            // We create a second ServerConnector, passing in the http configuration
            // we just made along with the previously created ssl context factory.
            // Next we set the port and a longer idle timeout.
            connector =
                    new ServerConnector(
                            server,
                            new SslConnectionFactory(
                                    sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                            new HttpConnectionFactory(https_config));
            connector.setIdleTimeout(500000);
        } else {
            connector = new ServerConnector(server, new HttpConnectionFactory(http_config));
            connector.setIdleTimeout(30000);
        }
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});
        return server;
    }

    /**
     * create handler for BOSH. for a different handler setup, override in a subclass. for more than
     * one handler, add them to a org.eclipse.jetty.server.handler.ContextHandlerCollection and
     * return the collection
     */
    protected Handler createHandler() {
        ServletContextHandler boshContext =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        boshContext.setContextPath(contextPath);
        BoshServlet boshServlet = new BoshServlet();
        boshServlet.setServerRuntimeContext(serverRuntimeContext);
        boshServlet.setAccessControlAllowOrigin(accessControlAllowOrigin);
        boshContext.addServlet(new ServletHolder(boshServlet), "/");
        return boshContext;
    }

    /**
     * @throws RuntimeException a wrapper of the possible {@link java.lang.Exception} that Jetty can
     *     throw at start-up
     */
    public void start() throws IOException {
        server = createJettyServer();
        Handler handler = createHandler();
        server.setHandler(handler);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.warn("Could not stop the Jetty server", e);
        }
    }
}
