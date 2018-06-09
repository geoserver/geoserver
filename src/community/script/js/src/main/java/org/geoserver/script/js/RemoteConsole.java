/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mozilla.javascript.Context;

public class RemoteConsole {

    private HttpClient client;
    private String sessionId;
    private BufferedReader in;
    private URL url;

    public RemoteConsole(BufferedReader in, URL url, String user, String password) {
        this.in = in;
        this.url = url;
        client = new HttpClient();

        // set up auth
        AuthScope scope = new AuthScope(url.getHost(), url.getPort());
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, password);
        client.getState().setCredentials(scope, creds);
        client.getParams().setAuthenticationPreemptive(true);

        sessionId = createSession();
        try {
            processInput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String collectInput(Context cx) throws IOException {
        String source = "";
        // Collect lines of source to compile.
        while (true) {
            String newline;
            newline = in.readLine();
            if (newline == null) {
                // hit EOF
                source = null;
                break;
            }
            source = source + newline + "\n";
            if (cx.stringIsCompilableUnit(source)) {
                break;
            }
        }
        return source;
    }

    private void processInput() throws IOException {
        boolean hitEOF = false;
        Context cx = Context.enter();
        try {
            while (!hitEOF) {
                System.err.print("js> ");
                System.err.flush();
                String input = collectInput(cx);
                if (input == null) {
                    hitEOF = true;
                    break;
                }
                if (!input.isEmpty()) {
                    String result = eval(input);
                    System.err.println(result);
                }
            }
        } finally {
            Context.exit();
        }
    }

    private String eval(String input) {
        BufferedReader reader;
        String sessionUrl = url.toString() + sessionId;
        PutMethod method = new PutMethod(sessionUrl);
        String result = "";
        try {
            RequestEntity entity = new StringRequestEntity(input, "text/plain", "UTF-8");
            method.setRequestEntity(entity);
            client.executeMethod(method);
            reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
            String line;
            while (((line = reader.readLine()) != null)) {
                result = result + line;
            }
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            method.releaseConnection();
        }
        return result;
    }

    private String createSession() {
        String sessionUrl = url.toString();
        PostMethod method = new PostMethod(sessionUrl);
        String id;
        try {
            client.executeMethod(method);
            id = method.getResponseBodyAsString();
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            method.releaseConnection();
        }
        return id;
    }

    public static void main(String args[]) throws MalformedURLException {
        URL url = new URL(args[0]);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        new RemoteConsole(input, url, args[1], args[2]);
    }
}
