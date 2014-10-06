/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import static org.geoserver.geosearch.rest.Properties.INDEXING_ENABLED;
import static org.geoserver.geosearch.rest.Properties.LAST_MODIFIED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.MediaType;
import org.springframework.util.Assert;

/**
 * REST format that produces the sitemap.xml document
 */
public class SiteMapXMLFormat extends StreamDataFormat {

    private static final String GEO_NS = "http://www.google.com/geo/schemas/sitemap/1.0";

    private static final String SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    private static final DateFormat LAST_MOD_FORMATTER = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZ");

    private static final DateFormat LAST_MOD_FORMATTER_GMT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        LAST_MOD_FORMATTER_GMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * {@code <http://localhost:8080/geoserver/geosearch>[/]} or whatever the base URL is, in order
     * to append {@code [/]l<layer>.kml} to it for each layer
     */
    private final String baseUrl;

    public SiteMapXMLFormat(final String baseUrl) {
        super(MediaType.TEXT_XML);
        this.baseUrl = baseUrl;
    }

    /**
     * Unsupported.
     * 
     * @see org.geoserver.rest.format.StreamDataFormat#read(java.io.InputStream)
     */
    @Override
    protected Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes out the sitemap to the given output stream.
     * 
     * @see org.geoserver.rest.format.StreamDataFormat#write(java.lang.Object, java.io.OutputStream)
     */
    @Override
    protected void write(final Object object, OutputStream out) throws IOException {
        Assert.isTrue(object instanceof Catalog);

        final Catalog catalog = (Catalog) object;

        final XMLStreamWriter writer;
        try {
            XMLOutputFactory factory;
            factory = XMLOutputFactory.newInstance();
            writer = factory.createXMLStreamWriter(out, "UTF-8");
        } catch (FactoryConfigurationError e) {
            throw new RuntimeException(e);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        try {
            encode(catalog, writer);
            writer.flush();
        } catch (XMLStreamException e) {
            throw (IOException) new IOException("Error encoding sitemap: " + e.getMessage())
                    .initCause(e);
        }
    }

    /**
     * @param catalog
     * @param writer
     * @throws XMLStreamException
     * @throws IOException
     */
    private void encode(Catalog catalog, XMLStreamWriter writer) throws XMLStreamException,
            IOException {

        writer.writeStartDocument();

        writer.writeStartElement("urlset");
        writer.writeDefaultNamespace(SITEMAP_NS);
        writer.writeNamespace("geo", GEO_NS);
        writer.setDefaultNamespace(SITEMAP_NS);

        writer.setPrefix("geo", GEO_NS);
        for (LayerInfo info : catalog.getLayers()) {
            final MetadataMap metadata = info.getMetadata();
            final Boolean indexingEnabled = metadata.get(INDEXING_ENABLED, Boolean.class);
            if (null == indexingEnabled || Boolean.FALSE.equals(indexingEnabled)) {
                continue;
            }
            final String layerName = info.getResource().getPrefixedName();

            writeUrl(writer, metadata, layerName);
        }
        for (LayerGroupInfo info : catalog.getLayerGroups()) {
            final MetadataMap metadata = info.getMetadata();
            final Boolean indexingEnabled = metadata.get(INDEXING_ENABLED, Boolean.class);
            if (null == indexingEnabled || Boolean.FALSE.equals(indexingEnabled)) {
                continue;
            }
            final String layerName = info.getName();

            writeUrl(writer, metadata, layerName);
        }
        writer.writeEndElement();

        writer.writeEndDocument();
    }

    /**
     * @param writer
     * @param metadata
     * @param layerName
     * @throws XMLStreamException
     */
    private void writeUrl(XMLStreamWriter writer, final MetadataMap metadata, final String layerName)
            throws XMLStreamException {
        writer.writeStartElement("url");
        {
            writer.writeStartElement("loc");
            writer.writeCharacters(kmlMetadataUrl(layerName));
            writer.writeEndElement();

            Long lastMod = metadata.get(LAST_MODIFIED, Long.class);
            if (lastMod != null) {
                Date parsed;
                try {
                    parsed = LAST_MOD_FORMATTER_GMT.parse(LAST_MOD_FORMATTER.format(new Date(
                            lastMod.longValue())));
                } catch (ParseException e) {
                    parsed = new Date(lastMod.longValue());
                }
                writer.writeStartElement("lastmod");
                writer.writeCharacters(LAST_MOD_FORMATTER_GMT.format(parsed));
                writer.writeEndElement();
            }

            writer.writeStartElement(GEO_NS, "geo");
            {
                writer.writeStartElement(GEO_NS, "format");
                writer.writeCharacters("kml");
                writer.writeEndElement();
            }
            writer.writeEndElement();

        }
        writer.writeEndElement();
    }

    private String kmlMetadataUrl(final String prefixedName) {
        final String encodedName = ResponseUtils.urlEncode(prefixedName) + ".kml";
        String path = ResponseUtils.appendPath(baseUrl, encodedName);
        return path;
    }

}
