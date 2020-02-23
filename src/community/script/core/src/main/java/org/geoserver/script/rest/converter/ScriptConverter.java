/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.converter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamCatalogListConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * This class enables us to extend the XMLXStreamListConverter and JSONXStreamListConverter classes
 * so we can override the default XML and JSON output of the REST module. Because these message
 * converters have a lower priority than the converters in the REST module, they will always be
 * called and therefore to prevent affecting the expected behavior of the REST module we are
 * checking the request path and only customizing the output if the request is within the Scripts
 * module.
 */
public abstract class ScriptConverter extends XStreamCatalogListConverter {

    /** XML handling for Script lists */
    @Component
    public static class XMLXStreamScriptListConverter extends XMLXStreamListConverter {
        @Autowired HttpServletRequest request;

        @Override
        protected XStream createXStreamInstance() {
            return new SecureXStream();
        }

        @Override
        public void encodeLink(String link, HierarchicalStreamWriter writer) {
            if (checkPath(request)) {
                try {
                    link = URLDecoder.decode(link, "UTF-8");
                    encodeAlternateAtomLinkNoExt(link, writer);
                    writer.addAttribute("type", getMediaType());

                    writer.endNode();
                } catch (UnsupportedEncodingException e) {

                }
            } else {
                encodeAlternateAtomLink(link, writer);
            }
        }

        @Override
        public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
            if (checkPath(request)) {
                try {
                    link = URLDecoder.decode(link, "UTF-8");
                    encodeAlternateAtomLinkNoExt(link, writer);
                    writer.addAttribute("type", getMediaType());

                    writer.endNode();
                } catch (UnsupportedEncodingException e) {

                }
            } else {
                encodeAlternateAtomLink(link, writer);
            }
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_ATOM_XML_VALUE;
        }

        @Override
        public String getExtension() {
            return "xml";
        }
    }

    /** JSON handling for Script lists */
    @Component
    public static class JSONXStreamScriptListConverter extends JSONXStreamListConverter {
        @Autowired HttpServletRequest request;

        @Override
        public void encodeLink(String link, HierarchicalStreamWriter writer) {
            if (checkPath(request)) {
                try {
                    link = URLDecoder.decode(link, "UTF-8");
                    writer.startNode("href");
                    writer.setValue(hrefNoExt(link));
                    writer.endNode();
                } catch (UnsupportedEncodingException e) {
                    throw new RestException(
                            e.getMessage().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                writer.startNode("href");
                writer.setValue(href(link));
                writer.endNode();
            }
        }

        @Override
        public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
            writer.setValue(href(link));
        }

        @Override
        protected XStream createXStreamInstance() {
            return new XStream(new JettisonMappedXmlDriver());
        }

        @Override
        public String getExtension() {
            return "json";
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }
    }

    /**
     * The default href method adds an extension onto the link which for the Scripts module is not
     * wanted This method is called in place of the default href method.
     */
    protected static String hrefNoExt(String link) {
        final RequestInfo pg = RequestInfo.get();

        // encode as relative or absolute depending on the link type
        if (link.startsWith("/")) {
            // absolute, encode from "root"
            return pg.servletURI(link);
        } else {
            // encode as relative
            return pg.pageURI(link);
        }
    }

    /**
     * The default encodeAlternateAtomLink method calls the default href method and adds an
     * extension to the link, this method is used to override that behavior.
     */
    protected static void encodeAlternateAtomLinkNoExt(
            String link, HierarchicalStreamWriter writer) {
        writer.startNode("atom:link");
        writer.addAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
        writer.addAttribute("rel", "alternate");
        writer.addAttribute("href", hrefNoExt(link));
    }

    protected static boolean checkPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("scripts/apps")
                || (path.contains("scripts/function"))
                || (path.contains("scripts/wfs/tx"))
                || (path.contains("scripts/wps"))) {
            return true;
        } else {
            return false;
        }
    }
}
