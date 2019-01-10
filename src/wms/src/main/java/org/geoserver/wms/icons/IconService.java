/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/** Custom servlet/controller for rendering KML icons. */
public class IconService extends AbstractController {

    static Logger LOG = Logging.getLogger(IconService.class);

    static Pattern URI = Pattern.compile("/icon/(?:([^/]+)/)?([^/]+)/?");

    private final Catalog catalog;

    public IconService(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public ModelAndView handleRequestInternal(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String path = request.getPathInfo();
        Matcher m = URI.matcher(path);
        if (!m.matches()) {
            response.sendError(
                    400, "Bad request, path must be of form: /icons/[<workspace>/]<style>");
            return null;
        }
        // this is wrong - matches 2 even when no workspace in url!
        String workspace = null, styleName = null;
        if (m.groupCount() == 2) {
            workspace = m.group(1);
            styleName = m.group(2);
        } else {
            styleName = m.group(1);
        }

        StyleInfo styleInfo =
                workspace != null
                        ? catalog.getStyleByName(workspace, styleName)
                        : catalog.getStyleByName(styleName);
        if (styleInfo == null) {
            String msg = "No such style " + styleName;
            if (workspace != null) {
                msg += " in workspace " + workspace;
            }
            response.sendError(404, msg);
            return null;
        }

        String q = request.getQueryString();
        try {
            Style style = styleInfo.getStyle();
            Map<String, Object> properties =
                    q != null ? KvpUtils.parseQueryString("?" + q) : Collections.EMPTY_MAP;
            Map<String, String> kvp = new HashMap<String, String>();
            for (String key : properties.keySet()) {
                Object value = properties.get(key);
                if (value instanceof String) {
                    kvp.put(key, (String) value);
                } else {
                    String[] values = (String[]) value;
                    kvp.put(key, values[0]);
                }
            }

            Style adjustedStyle = IconPropertyInjector.injectProperties(style, kvp);

            BufferedImage image = IconRenderer.renderIcon(adjustedStyle);

            response.setContentType("image/png");
            ImageIO.write(image, "PNG", response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            String msg = "Failed to load style: " + workspace + " " + styleName;
            response.sendError(500, msg + ", " + e.getMessage());
            LOG.log(Level.WARNING, msg, e);
        }

        return null;
    }
}
