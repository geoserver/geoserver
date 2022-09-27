/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.gen.info;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.gen.info.GeneralizationInfos;
import org.geotools.util.URLs;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Replacement of the geotools default implementation to use the GeoServer Resource loader to be
 * agnostic of how the resources are stored (on disk or on database) With this implementation it is
 * possible to store the external configuration file into a JDBC Resource store for example
 *
 * @author Christian Mueller
 */
public class GeneralizationInfosProviderImpl
        extends org.geotools.data.gen.info.GeneralizationInfosProviderImpl {

    /**
     * Override the default implementation to use the GeoServer Resource loader to be agnostic of
     * how the resources are stored (on disk or on database)
     */
    @Override
    protected URL deriveURLFromSourceObject(Object source) throws IOException {
        if (source == null) {
            throw new IOException("Cannot read from null");
        }

        if (source instanceof String) {
            String path = (String) source;

            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource resource = loader.get(Paths.convert(path));

            URL url = null;
            switch (resource.getType()) {
                case RESOURCE:
                    url = URLs.fileToUrl(resource.file());
                    break;
                case UNDEFINED:
                    url = new URL(path);
                    break;
                default:
                    throw new IOException("Trying to use for configuration directory " + source);
            }

            url = new URL(URLDecoder.decode(url.toExternalForm(), "UTF8"));
            return url;
        }
        throw new IOException("Cannot read from " + source);
    }

    /**
     * Override the default implementation to use the GeoServer Resource loader to be agnostic of
     * how the resources are stored (on disk or on database)
     */
    @Override
    protected GeneralizationInfos parseXML(URL url) throws IOException {

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource configurationResource = loader.fromURL(url);

        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);

        try (InputStream in = configurationResource.in()) {
            DocumentBuilder db = factory.newDocumentBuilder();
            doc = db.parse(in);
            VALIDATOR.validate(new DOMSource(doc));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        NodeList nl = doc.getElementsByTagName(GENERALIZATION_INFOS_TAG);
        GeneralizationInfos gInfos = new GeneralizationInfos();

        Node gInfosNode = nl.item(0);
        checkVersion(gInfosNode);

        NamedNodeMap attrMap = gInfosNode.getAttributes();
        if (attrMap.getNamedItem(DATASOURCE_NAME_ATTR) != null)
            gInfos.setDataSourceName(attrMap.getNamedItem(DATASOURCE_NAME_ATTR).getTextContent());
        if (attrMap.getNamedItem(DATASOURCE_NAMESPACE_NAME_ATTR) != null)
            gInfos.setDataSourceNameSpace(
                    attrMap.getNamedItem(DATASOURCE_NAMESPACE_NAME_ATTR).getTextContent());
        parseGeneralizationInfoNodes(gInfosNode, gInfos);
        return gInfos;
    }
}
