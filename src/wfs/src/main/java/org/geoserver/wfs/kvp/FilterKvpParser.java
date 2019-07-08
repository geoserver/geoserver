/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.gml.GMLFilterDocument;
import org.geotools.gml.GMLFilterGeometry;
import org.geotools.xml.filter.FilterFilter;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.opengis.filter.Filter;
import org.vfny.geoserver.util.requests.FilterHandlerImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;

/**
 * A base {@code FILTER} parameter parser that expects a subclass to provide the actual {@link
 * Parser} configuration for the expected OGC Filter Encoding spec version.
 *
 * @author Justin Deoliveira
 * @author Gabriel Roldan
 */
public abstract class FilterKvpParser extends KvpParser {

    private EntityResolverProvider entityResolverProvider;

    public FilterKvpParser(GeoServer geoServer) {
        super("filter", List.class);
        this.entityResolverProvider = new EntityResolverProvider(geoServer);
    }

    /**
     * Subclasses shall implement to provide the parse() method the appropriate parser Configuration
     * for the filter spec version they specialize on.
     *
     * @return The Configuration for the appropriate Filter spec version.
     */
    protected abstract Configuration getParserConfiguration();

    public Object parse(String value) throws Exception {
        // create the parser
        final Configuration configuration = getParserConfiguration();
        final Parser parser = new Parser(configuration);
        parser.setEntityResolver(entityResolverProvider.getEntityResolver());

        // seperate the individual filter strings
        List unparsed = KvpUtils.readFlat(value, KvpUtils.OUTER_DELIMETER);
        List filters = new ArrayList();

        Iterator i = unparsed.listIterator();

        while (i.hasNext()) {
            String string = (String) i.next();
            if ("".equals(string.trim())) {
                filters.add(Filter.INCLUDE);
            } else {
                try {
                    Filter filter = (Filter) parser.parse(new StringReader(string));

                    if (filter == null) {
                        throw new NullPointerException();
                    }

                    filters.add(filter);
                } catch (Exception e) {
                    // parsing failed, fall back to old parser
                    String msg = "Unable to parse filter: " + string;
                    LOGGER.log(Level.WARNING, msg, e);

                    Filter filter = parseXMLFilterWithOldParser(new StringReader(string));

                    if (filter != null) {
                        filters.add(filter);
                    }
                }
            }
        }

        return filters;
    }

    /**
     * Reads the Filter XML request into a geotools Feature object.
     *
     * <p>This uses the "old" filter parser and is around to maintain some backwards compatability
     * with cases in which the new parser chokes on a filter that hte old one could handle.
     *
     * @param rawRequest The plain POST text from the client.
     * @return The geotools filter constructed from rawRequest.
     * @throws WfsException For any problems reading the request.
     */
    protected Filter parseXMLFilterWithOldParser(Reader rawRequest) throws ServiceException {
        // translate string into a proper SAX input source
        InputSource requestSource = new InputSource(rawRequest);

        // instantiante parsers and content handlers
        FilterHandlerImpl contentHandler = new FilterHandlerImpl();
        contentHandler.setEntityResolver(entityResolverProvider.getEntityResolver());
        FilterFilter filterParser = new FilterFilter(contentHandler, null);
        GMLFilterGeometry geometryFilter = new GMLFilterGeometry(filterParser);
        GMLFilterDocument documentFilter = new GMLFilterDocument(geometryFilter);

        // read in XML file and parse to content handler
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ParserAdapter adapter = new ParserAdapter(parser.getParser());
            adapter.setEntityResolver(entityResolverProvider.getEntityResolver());
            adapter.setContentHandler(documentFilter);
            adapter.parse(requestSource);
            LOGGER.fine("just parsed: " + requestSource);
        } catch (SAXException e) {
            throw new ServiceException(
                    e,
                    "XML getFeature request SAX parsing error",
                    XmlRequestReader.class.getName());
        } catch (IOException e) {
            throw new ServiceException(
                    e, "XML get feature request input error", XmlRequestReader.class.getName());
        } catch (ParserConfigurationException e) {
            throw new ServiceException(
                    e, "Some sort of issue creating parser", XmlRequestReader.class.getName());
        }

        LOGGER.fine("passing filter: " + contentHandler.getFilter());

        return contentHandler.getFilter();
    }
}
