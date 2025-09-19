package org.geoserver.web.security.oauth2.intgration.keycloak;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class WebRequests {

    public static WebResponse webRequestGET(String uri) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        var response = new WebResponse(connection);
        connection.disconnect();
        return response;
    }

    public static WebResponse webRequestPOSTForm(String uri, String body, CookieManager cookieManager)
            throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true); // Enable output for sending data
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        var cookieVal = String.join(
                "; ",
                cookieManager.getCookieStore().getCookies().stream()
                        .map(x -> x.getName() + "=" + x.getValue())
                        .toList());
        if (cookieManager.getCookieStore().getCookies().size() > 0) {
            connection.setRequestProperty("Cookie", cookieVal);
        }
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }
        connection.setInstanceFollowRedirects(false);
        System.out.println("curl -H \"Content-Type:application/x-www-form-urlencoded\" -d \"" + body + "\"  \\");
        System.out.println("     -H \"Cookie: " + cookieVal + "\"  \\");
        System.out.println("     \"" + uri + "\" -v");
        var response = new WebResponse(connection);
        connection.disconnect();
        return response;
    }

    public static class WebResponse {

        public Map<String, List<String>> headers;
        public int statusCode;
        public String body = "";
        public HttpURLConnection connection;
        public CookieManager cookieManager;

        public WebResponse(HttpURLConnection connection) throws IOException {
            statusCode = connection.getResponseCode();
            body = IOUtils.toString(getInputStream(connection), StandardCharsets.UTF_8);
            //            if (statusCode == 200) {
            //                body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            //            } else {
            //                body = IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
            //            }
            headers = connection.getHeaderFields();

            cookieManager = new CookieManager();
            List<String> cookies = headers.get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    cookieManager
                            .getCookieStore()
                            .add(null, HttpCookie.parse(cookie).get(0));
                }
            }
        }

        public InputStream getInputStream(HttpURLConnection connection) throws IOException {
            try {
                return connection.getInputStream();
            } catch (Exception e) {
                return connection.getErrorStream();
            }
        }
    }
}
