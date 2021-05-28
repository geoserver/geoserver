/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 * This class provides the necessary logic to retrieve a reader based on the extension value passed.
 */
public class TemplateReaderProvider {
    enum SupportedExtension {
        JSON,
        XML,
        XHTML
    }

    /**
     * Find the proper TemplateReader.
     *
     * @param resourceExtension the resource file extension can be xml, xhtml, json.
     * @param is the input stream of the resource to be parsed.
     * @param configuration the TemplateReaderConfiguration.
     * @return
     * @throws IOException
     */
    public static TemplateReader findReader(
            String resourceExtension, InputStream is, TemplateReaderConfiguration configuration)
            throws IOException {
        TemplateReader reader;
        if (resourceExtension.equalsIgnoreCase(SupportedExtension.JSON.name())) {
            ObjectMapper mapper =
                    new ObjectMapper(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
            reader = new JSONTemplateReader(mapper.readTree(is), configuration);
        } else if (resourceExtension.equalsIgnoreCase(SupportedExtension.XHTML.name())
                || resourceExtension.equalsIgnoreCase(SupportedExtension.XML.name())) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
            XMLEventReader eventReader = null;
            try {
                eventReader = xmlInputFactory.createXMLEventReader(is);
            } catch (XMLStreamException e) {
                throw new IOException(e);
            }
            reader = new XMLTemplateReader(eventReader, configuration.getNamespaces());
        } else {
            throw new UnsupportedOperationException(
                    "Not a supported extension " + resourceExtension);
        }
        return reader;
    }
}
