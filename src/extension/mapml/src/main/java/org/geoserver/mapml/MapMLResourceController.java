/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Controller to serve MapML static resources (JavaScript, CSS, etc.) */
@Controller
public class MapMLResourceController {

    private static final int BUFFER_SIZE = 8192;

    @GetMapping("/mapml/viewer/**")
    public void serveViewerResource(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String resourcePath = path.substring(path.indexOf("/mapml/viewer/") + "/mapml/viewer/".length());
        serveResource(response, "/viewer/" + resourcePath);
    }

    @GetMapping("/mapml/js/**")
    public void serveJsResource(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String resourcePath = path.substring(path.indexOf("/mapml/js/") + "/mapml/js/".length());
        serveResource(response, "/org/geoserver/mapml/" + resourcePath);
    }

    private void serveResource(HttpServletResponse response, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Set content type based on file extension
            String contentType = getContentType(resourcePath);
            response.setContentType(contentType);

            // Set cache headers for static resources
            response.setHeader("Cache-Control", "public, max-age=3600");

            try (OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
        }
    }

    private String getContentType(String resourcePath) {
        if (resourcePath.endsWith(".js")) {
            return "application/javascript";
        } else if (resourcePath.endsWith(".css")) {
            return "text/css";
        } else if (resourcePath.endsWith(".json")) {
            return "application/json";
        } else if (resourcePath.endsWith(".png")) {
            return "image/png";
        } else if (resourcePath.endsWith(".jpg") || resourcePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (resourcePath.endsWith(".gif")) {
            return "image/gif";
        } else if (resourcePath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (resourcePath.endsWith(".html")) {
            return "text/html";
        } else {
            return "application/octet-stream";
        }
    }
}
