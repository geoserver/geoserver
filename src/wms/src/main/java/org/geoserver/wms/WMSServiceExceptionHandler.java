/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.util.Version;

/**
 * An implementation of {@link ServiceExceptionHandler} which outputs as service exception in a
 * <code>ServiceExceptionReport</code> document.
 *
 * <p>
 *
 * <h3>Version</h3>
 *
 * By default this exception handler will output a <code>ServiceExceptionReport</code> which is of
 * version <code>1.2.0</code>. This may be overriden with {@link #setVersion(String)}.
 *
 * <p>
 *
 * <h3>DTD and Schema</h3>
 *
 * By default, no DTD or XML Schema reference will be included in the document. The methods {@link
 * #setDTDLocation(String)} and {@link #setSchemaLocation(String)} can be used to override this
 * behaviour. Only one of these methods should be set per instance of this class.
 *
 * <p>The supplied value should be relative to the web application context root.
 *
 * <p>
 *
 * <h3>Content Type</h3>
 *
 * The default content type for the created document is <code>text/xml</code>, this can be
 * overridden with {@link #setContentType(String)}.
 *
 * @author Justin Deoliveira
 * @author Gabriel Roldan
 * @author Carlo Cancellieri - GeoSolutions
 */
public class WMSServiceExceptionHandler extends ServiceExceptionHandler {

    static final Set<String> FORMATS =
            new HashSet<String>(
                    Arrays.asList("image/png", "image/png8", "image/gif", "image/jpeg"));

    /** Map from content type to ImageIO format name for {@link ImageIO#write} */
    static final Map<String, String> IMAGEIO_FORMATS =
            new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;

                {
                    put("image/png", "png");
                    put("image/png8", "png");
                    put("image/gif", "gif");
                    put("image/jpeg", "jpeg");
                }
            };

    private GeoServer geoServer;

    /**
     * Creates a new exception handler for WMS exceptions
     *
     * @param services the {@link WMSInfo}s this handler writes exceptions for
     * @param geoServer needed to know whether to write detailed exception reports or not (as per
     *     {@code GeoServer.getGlobal().isVerbose()})
     */
    public WMSServiceExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        // first of all check what kind of exception handling we must perform
        final String exceptions;
        final int width;
        final int height;
        final String format;
        final Color bgcolor;
        final Boolean transparent;
        try {
            exceptions = (String) request.getKvp().get("EXCEPTIONS");
            if (exceptions == null) {
                // use default
                handleXmlException(exception, request);
                return;
            }
        } catch (Exception e) {
            // width and height might be missing
            handleXmlException(exception, request);
            return;
        }
        boolean verbose = geoServer.getSettings().isVerboseExceptions();
        String charset = geoServer.getSettings().getCharset();
        if (JSONType.isJsonMimeType(exceptions)) {
            // use Json format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, false);
            return;
        } else if (JSONType.useJsonp(exceptions)) {
            // use JsonP format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, true);
            return;
        } else if (isImageExceptionType(exceptions)) {
            // ok, it's image, then we have to build a text representing the
            // exception and lay it out in the image
            try {
                width = (Integer) request.getKvp().get("WIDTH");
                height = (Integer) request.getKvp().get("HEIGHT");
                format = (String) request.getKvp().get("FORMAT");
                bgcolor = (Color) request.getKvp().get("BGCOLOR");
                transparent = (Boolean) request.getKvp().get("TRANSPARENT");
                if (width > 0 && height > 0 && FORMATS.contains(format)) {
                    handleImageException(
                            exception,
                            request,
                            width,
                            height,
                            format,
                            exceptions,
                            bgcolor,
                            transparent);
                    return;
                } else {
                    // use default
                    handleXmlException(exception, request);
                }
            } catch (Exception e) {
                // width and height might be missing
                // use default
                handleXmlException(exception, request);
            }
        } else if (isPartialMapExceptionType(exceptions)) {
            try {
                format = (String) request.getKvp().get("FORMAT");
                if (exception instanceof WMSPartialMapException && FORMATS.contains(format)) {
                    handlePartialMapException(exception, request, format);
                } else {
                    handleXmlException(exception, request);
                }
            } catch (Exception e) {
                // may have invalid exception type (not WMSServiceException)
                // use default
                handleXmlException(exception, request);
            }
        } else {
            // use default
            handleXmlException(exception, request);
        }
    }

    public static boolean isImageExceptionType(String exceptions) {
        return "application/vnd.ogc.se_inimage".equals(exceptions)
                || "INIMAGE".equals(exceptions)
                || "BLANK".equals(exceptions)
                || "application/vnd.ogc.se_blank".equals(exceptions);
    }

    private void handleImageException(
            ServiceException exception,
            Request request,
            final int width,
            final int height,
            final String format,
            String exceptionFormat,
            Color bgcolor,
            Boolean transparent) {

        if (("BLANK".equals(exceptionFormat)
                        || "application/vnd.ogc.se_blank".equals(exceptionFormat))
                && bgcolor == null
                && Boolean.TRUE.equals(transparent)) {
            bgcolor = new Color(0, 0, 0, 0);
        }

        if (bgcolor == null) {
            bgcolor = Color.WHITE;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) img.getGraphics();

        g.setColor(bgcolor);
        g.addRenderingHints(
                Collections.singletonMap(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        if (!("BLANK".equals(exceptionFormat)
                || "application/vnd.ogc.se_blank".equals(exceptionFormat))) { // wms 1.3 only
            g.setColor(Color.BLACK);

            // draw the exception text (give it a good offset so that it can be read
            // properly in the OL preview as well)
            paintLines(g, buildImageExceptionText(exception), width - 2, 35, 5);
        }

        // encode
        g.dispose();
        try {
            final HttpServletResponse response = request.getHttpResponse();
            if ("image/png8".equals(format)) {
                response.setContentType("image/png");
            } else {
                response.setContentType(format);
            }
            final ServletOutputStream os = response.getOutputStream();
            ImageIO.write(img, IMAGEIO_FORMATS.get(format), os);
            os.flush();
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        }
    }

    public static boolean isPartialMapExceptionType(String exceptions) {
        return "application/vnd.gs.wms_partial".equals(exceptions)
                || "PARTIALMAP".equals(exceptions);
    }

    private void handlePartialMapException(
            ServiceException exception, Request request, String format) {
        RenderedImageMap map = (RenderedImageMap) ((WMSPartialMapException) exception).getMap();
        try {
            final HttpServletResponse response = request.getHttpResponse();
            if ("image/png8".equals(format)) {
                response.setContentType("image/png");
            } else {
                response.setContentType(format);
            }

            final ServletOutputStream os = response.getOutputStream();
            ImageIO.write(map.getImage(), IMAGEIO_FORMATS.get(format), os);
            os.flush();
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        }
    }

    public void handleXmlException(ServiceException exception, Request request) {
        // Location of document type defintion for document
        String dtdLocation = null;

        // Location of schema for document.
        String schemaLocation = null;

        // The content type of the produced document
        String contentType;

        // first off negotiate the version to see what version of exception report to return
        Version version = WMS.negotiateVersion(request.getVersion());
        if (version == WMS.VERSION_1_1_1) {
            // use dtd style
            dtdLocation = "wms/1.1.1/WMS_exception_1_1_1.dtd";
            contentType = "application/vnd.ogc.se_xml";
        } else {
            // use xml schema
            schemaLocation = "wms/1.3.0/exceptions_1_3_0.xsd";
            contentType = "text/xml";
        }

        String tab = "   ";
        StringBuffer sb = new StringBuffer();

        // xml header TODO: should the encoding the server default?
        sb.append("<?xml version=\"1.0\"");
        sb.append(" encoding=\"UTF-8\"");

        if (dtdLocation != null) {
            sb.append(" standalone=\"no\"");
        }

        sb.append("?>");

        // dtd location
        if (dtdLocation != null) {
            String fullDtdLocation = buildSchemaURL(baseURL(request.getHttpRequest()), dtdLocation);
            sb.append("<!DOCTYPE ServiceExceptionReport SYSTEM \"" + fullDtdLocation + "\"> ");
        }

        // root element
        sb.append("<ServiceExceptionReport version=\"" + version.toString() + "\" ");

        // xml schema location
        if ((schemaLocation != null) && (dtdLocation == null)) {
            String fullSchemaLocation =
                    buildSchemaURL(baseURL(request.getHttpRequest()), schemaLocation);

            sb.append("xmlns=\"http://www.opengis.net/ogc\" ");
            sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
            sb.append(
                    "xsi:schemaLocation=\"http://www.opengis.net/ogc " + fullSchemaLocation + "\"");
        }

        sb.append(">");

        // write out the service exception
        sb.append(tab + "<ServiceException");

        // exception code
        if ((exception.getCode() != null) && !exception.getCode().equals("")) {
            sb.append(" code=\"" + ResponseUtils.encodeXML(exception.getCode()) + "\"");
        }

        // exception locator
        if ((exception.getLocator() != null) && !exception.getLocator().equals("")) {
            sb.append(" locator=\"" + ResponseUtils.encodeXML(exception.getLocator()) + "\"");
        }

        sb.append(">");

        // message
        if ((exception.getMessage() != null)) {
            sb.append("\n" + tab + tab);
            OwsUtils.dumpExceptionMessages(exception, sb, true);

            if (geoServer.getSettings().isVerboseExceptions()) {
                ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                exception.printStackTrace(new PrintStream(stackTrace));

                sb.append("\nDetails:\n");
                sb.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
            }
        }

        sb.append("\n</ServiceException>");
        sb.append("</ServiceExceptionReport>");

        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(contentType);

        // TODO: server encoding?
        response.setCharacterEncoding("UTF-8");

        try {
            response.getOutputStream().write(sb.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            // throw new RuntimeException(e);
            // Hmm, not much we can do here. I guess log the fact that we couldn't write out the
            // exception and be done with it...
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        }
    }

    private String buildImageExceptionText(ServiceException exception) {
        StringBuffer sb = new StringBuffer();
        // exception code and locator
        if ((exception.getCode() != null) && !exception.getCode().equals("")) {
            sb.append("code=\"" + exception.getCode() + "\"");
        }

        // exception locator
        if ((exception.getLocator() != null) && !exception.getLocator().equals("")) {
            sb.append(" locator=\"" + exception.getLocator() + "\"");
        }

        // message
        if ((exception.getMessage() != null)) {
            OwsUtils.dumpExceptionMessages(exception, sb, false);

            if (geoServer.getSettings().isVerboseExceptions()) {
                ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                exception.printStackTrace(new PrintStream(stackTrace));

                sb.append("\nDetails:\n");
                sb.append(new String(stackTrace.toByteArray()));
            }
        }

        return sb.toString();
    }

    /**
     * Paint the provided text onto the graphics wrapping words at the specified lineWidth.
     *
     * @param g the Graphics2D which will be used to draw the text
     * @param text the text to render
     * @param lineWidth the width of the area where words should be rendered
     * @param startX an offset from the left edge of the image to where text should start
     * @param startY an offset from the top edge of the image to where text should start
     */
    void paintLines(Graphics2D g, String text, int lineWidth, int startX, int startY) {
        // split the text into lines, LineBreakMeasurer only lays out the single
        // line
        String[] lines = text.split("\\n");

        // setup the cursor
        Point cursor = new Point(startX, startY);

        // grab the line height to skip empty lines
        final FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();

        FontRenderContext frc = g.getFontRenderContext();

        // scan over the
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];

            if ("".equals(line)) {
                cursor.y += lineHeight;
            } else {
                AttributedString styledText = new AttributedString(line);
                LineBreakMeasurer measurer = new LineBreakMeasurer(styledText.getIterator(), frc);

                while (measurer.getPosition() < line.length()) {

                    TextLayout layout = measurer.nextLayout(lineWidth - startX);

                    cursor.y += (layout.getAscent());
                    float dx = layout.isLeftToRight() ? 0 : (lineWidth - layout.getAdvance());

                    layout.draw(g, cursor.x + dx, cursor.y);
                    cursor.y += layout.getDescent() + layout.getLeading();
                }
            }
        }
    }
}
