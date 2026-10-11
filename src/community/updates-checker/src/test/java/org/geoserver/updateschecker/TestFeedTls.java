/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Local HTTPS feed servers for tests. The module only ever fetches over https, so the tests do too: the server uses a
 * throw-away self-signed certificate for {@code localhost} and {@code 127.0.0.1} (the keystore holds a test key and
 * nothing else), and the JVM default {@link SSLContext} is replaced by one that trusts it for as long as a server is
 * running, then put back. The module's connections pick it up, so no test hook is needed in the production code.
 */
public final class TestFeedTls {

    private static final SSLContext CONTEXT = createContext();

    private static int servers;
    private static SSLContext savedContext;
    private static SSLSocketFactory savedFactory;

    private TestFeedTls() {}

    private static SSLContext createContext() {
        try (InputStream in = TestFeedTls.class.getResourceAsStream("test-feed-tls.p12")) {
            char[] password = "changeit".toCharArray();
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, password);
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(store, password);
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("cannot set up the test certificate", e);
        }
    }

    /** @return an unstarted HTTPS server on a free loopback port; {@link #serverStopped()} must follow its last use. */
    public static synchronized HttpsServer server() throws Exception {
        if (servers++ == 0) {
            savedContext = SSLContext.getDefault();
            savedFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            // the module reads through HttpsURLConnection, which takes its socket factory from here
            SSLContext.setDefault(CONTEXT);
            HttpsURLConnection.setDefaultSSLSocketFactory(CONTEXT.getSocketFactory());
        }
        HttpsServer server =
                HttpsServer.create(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(CONTEXT));
        return server;
    }

    /** Puts the JVM defaults back once the last server obtained from {@link #server()} is done. */
    public static synchronized void serverStopped() {
        if (--servers == 0) {
            SSLContext.setDefault(savedContext);
            HttpsURLConnection.setDefaultSSLSocketFactory(savedFactory);
        }
    }

    /** @return the https URL of a path on the server. */
    public static String url(HttpsServer server, String path) {
        return "https://127.0.0.1:" + server.getAddress().getPort() + path;
    }
}
