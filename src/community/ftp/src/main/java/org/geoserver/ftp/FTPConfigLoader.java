/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerResourceLoader;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxWriter;

/**
 * Loads and saves the GeoSever FTP Service {@link FTPConfig configuration} from and to the
 * {@code ftp.xml} file inside the GeoServer data directory.
 * 
 * @author groldan
 * 
 */
class FTPConfigLoader {
    private static final String CONFIG_FILE_NAME = "ftp.xml";

    private GeoServerResourceLoader resourceLoader;

    public FTPConfigLoader(final GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public FTPConfig load() {
        InputStream in = null;
        try {
            File configFile = findConfigFile();
            if (!configFile.exists()) {
                FTPConfig ftpConfig = new FTPConfig();
                save(ftpConfig);
                return ftpConfig;
            }
            in = new FileInputStream(configFile);
            XStream xs = getPersister();
            FTPConfig config = (FTPConfig) xs.fromXML(in);
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private XStream getPersister() {
        Map<String, String> comments = new HashMap<String, String>();
        comments.put("enabled", "true to enable the FTP service, false to disable it");
        comments.put("ftpPort", "Port where the FTP Service listens for connections");
        comments.put("idleTimeout", "number of seconds during which no network activity "
                + "is allowed before a session is closed due to inactivity");
        comments.put("serverAddress",
                "IP Address used for binding the local socket. If unset, the server binds "
                        + "to all available network interfaces");
        comments.put("passivePorts", "Ports to be used for PASV data connections. Ports can "
                + "be defined as single ports, closed or open ranges:\n"
                + " Multiple definitions can be separated by commas, for example:\n"
                + "  2300 : only use port 2300 as the passive port\n"
                + "  2300-2399 : use all ports in the range\n"
                + "  2300- : use all ports larger than 2300\n"
                + "  2300, 2305, 2400- : use 2300 or 2305 or any port larger than 2400\n");

        comments.put("passiveAddress",
                "Address on which the server will listen to passive data connections, \n"
                        + "  if not set defaults to the same address as the control "
                        + "socket for the session.");
        comments.put(
                "passiveExternalAddress",
                "the address the server will claim to be listening on in the PASV reply.\n"
                        + " Useful when the server is behind a NAT firewall and the client sees a different address than the server is using.");

        HierarchicalStreamDriver streamDriver = new CommentingStaxWriter(comments);
        XStream xStream = new SecureXStream(streamDriver);
        xStream.alias("ftp", FTPConfig.class);
        xStream.allowTypes(new Class[] { FTPConfig.class });
        return xStream;
    }

    public void save(FTPConfig config) {
        File configFile;
        OutputStream out = null;
        try {
            configFile = findConfigFile();
            out = new FileOutputStream(configFile);
            XStream xs = getPersister();
            xs.toXML(config, new OutputStreamWriter(out, "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private File findConfigFile() throws IOException {
        File configFile = resourceLoader.find(CONFIG_FILE_NAME);
        if (configFile == null) {
            configFile = new File(resourceLoader.getBaseDirectory(), CONFIG_FILE_NAME);
        }
        return configFile;
    }

    private static final class CommentingStaxWriter extends StaxDriver {
        final QNameMap qnameMap = new QNameMap();

        private Map<String, String> comments;

        public CommentingStaxWriter(Map<String, String> elementComments) {
            this.comments = elementComments;
        }

        @Override
        public StaxWriter createStaxWriter(XMLStreamWriter out, boolean writeStartEndDocument)
                throws XMLStreamException {

            return new StaxWriter(qnameMap, out, writeStartEndDocument, isRepairingNamespace(),
                    xmlFriendlyReplacer()) {

                private int indentLevel = 0;

                @Override
                public void startNode(final String elementName) {
                    XMLStreamWriter xmlStreamWriter = super.getXMLStreamWriter();
                    try {
                        xmlStreamWriter.writeCharacters("\n");
                        String comment = comments.get(elementName);
                        if (comment != null) {
                            indent(xmlStreamWriter);
                            xmlStreamWriter.writeComment(comment);
                            xmlStreamWriter.writeCharacters("\n");
                            indent(xmlStreamWriter);
                        }
                    } catch (XMLStreamException ignored) {
                        // shouldn't happen
                    }
                    super.startNode(elementName);
                    indentLevel++;
                }

                private void indent(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
                    for (int i = 0; i < indentLevel; i++) {
                        xmlStreamWriter.writeCharacters("  ");
                    }
                }

                @Override
                public void endNode() {
                    indentLevel--;
                    //System.err.println("indentLevel: " + indentLevel);
                    if (indentLevel == 0) {
                        XMLStreamWriter xmlStreamWriter = super.getXMLStreamWriter();
                        try {
                            xmlStreamWriter.writeCharacters("\n");
                            super.endNode();
                            xmlStreamWriter.writeCharacters("\n");
                        } catch (XMLStreamException ignored) {
                            // shouldn't happen
                        }
                    } else {
                        super.endNode();
                    }
                }
            };

        }
    }

}
