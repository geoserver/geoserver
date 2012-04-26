package org.geoserver.geogit.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geogit.api.RevCommit;
import org.geoserver.geogit.GEOGIT;
import org.geotools.feature.type.DateUtil;
import org.gvsig.bxml.adapt.stax.XmlStreamWriterAdapter;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.gvsig.bxml.stream.EncodingOptions;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;

public class TimeLineRestlet extends Restlet {

    @Override
    public void handle(Request request, Response response) {
        super.init(request, response);
        if (!Method.GET.equals(request.getMethod())) {
            response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED, "Method not allowed");
        }

        final Iterator<RevCommit> commits;
        {
            GEOGIT facade = GEOGIT.get();
            try {
                commits = facade.getGeoGit().log().call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        response.setEntity(new OutputRepresentation(MediaType.TEXT_XML) {

            @Override
            public void write(final OutputStream out) throws IOException {
                BxmlStreamWriter w;
                {
                    XMLStreamWriter staxWriter;
                    try {
                        staxWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    } catch (FactoryConfigurationError e) {
                        throw new RuntimeException(e);
                    }
                    w = new XmlStreamWriterAdapter(new EncodingOptions(), staxWriter);
                }

                w.writeStartDocument();
                w.writeStartElement(XMLConstants.NULL_NS_URI, "data");

                while (commits.hasNext()) {
                    RevCommit e = commits.next();
                    w.writeStartElement(XMLConstants.NULL_NS_URI, "event");
                    {
                        String id = e.getId().toString();
                        String title = title(e.getMessage());
                        String summary = e.getMessage();
                        Date updated = new Date(e.getTimestamp());
                        String feedHref = "../../ows?service=GSS&version=1.0.0&request=GetEntries&feed=REPLICATIONFEED&outputFormat=text/xml&";
                        feedHref += "temporalOp=TEquals&startTime="
                                + DateUtil.serializeDateTime(updated.getTime(), true)
                                + "&startPosition=1&maxEntries=50";

                        w.writeStartAttribute(XMLConstants.NULL_NS_URI, "start");
                        w.writeValue(DateUtil.serializeDateTime(updated.getTime(), true));

                        w.writeStartAttribute(XMLConstants.NULL_NS_URI, "title");
                        w.writeValue(title);

                        w.writeStartAttribute(XMLConstants.NULL_NS_URI, "link");
                        w.writeValue(feedHref);

                        w.writeStartAttribute(XMLConstants.NULL_NS_URI, "image");
                        w.writeValue("../../web/resources/org.geoserver.geogit.web.VersionedLayersPage/feed-icon-14x14.png");

                        // this is the icon of the event in the timeline, might be useful to
                        // distinguish different types of events in the future
                        // w.writeStartAttribute(XMLConstants.NULL_NS_URI, "icon");
                        // w.writeValue("../../web/resources/org.geoserver.gss.web.ChangesPanel/feed-icon-14x14.png");

                        w.writeEndAttributes();

                        w.writeValue("<i>" + summary + "</i>");
                        w.writeValue("<p>Author: ");
                        if (e.getAuthor() != null) {
                            w.writeValue(e.getAuthor());
                        } else {
                            w.writeValue("<i>unknown</i>");
                        }
                        //
                        // w.writeValue("<p><a href=\""
                        // + feedHref
                        // +
                        // "\"><img src=\"../web/resources/org.geoserver.gss.web.ChangesPanel/feed-icon-14x14.png\"/> Open contents<a>");
                    }
                    w.writeEndElement();
                }

                w.writeEndElement();
                w.writeEndDocument();
                w.flush();
            }

            private String title(final String message) {
                if (message == null) {
                    return "<no commit message provided>";
                }
                String title = message.split("\\r?\\n")[0];
                if (title.length() > 35) {
                    title = title.substring(0, 35) + "...";
                }
                return title;
            }
        });
        response.setStatus(Status.SUCCESS_OK);
    }
}
