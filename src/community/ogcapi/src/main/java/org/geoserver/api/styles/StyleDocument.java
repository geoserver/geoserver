package org.geoserver.api.styles;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Collections;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.util.Version;
import org.springframework.http.MediaType;

@JsonPropertyOrder({"id", "title", "styles", "links"})
public class StyleDocument extends AbstractDocument {

    String id;
    String title;

    public StyleDocument(StyleInfo style) throws IOException {
        this.id = NCNameResourceCodec.encode(style);
        this.title = style.getStyle().getDescription().getTitle().toString();

        APIRequestInfo info = APIRequestInfo.get();

        // adding the links to various formats
        for (StyleHandler sh : Styles.handlers()) {
            // can we encode the style in this format?
            if ((style.getFormat() != null && sh.getFormat().equals(style.getFormat()))
                    || sh.supportsEncoding()) {
                // different versions have different mime types, create one link for each
                for (Version ver : sh.getVersions()) {
                    String styleURL =
                            buildURL(
                                    info.getBaseURL(),
                                    "ogc/styles/" + ResponseUtils.urlEncode(id),
                                    Collections.singletonMap("f", sh.mimeType(ver)),
                                    URLMangler.URLType.SERVICE);

                    Link link = new Link(styleURL, "stylesheet", sh.mimeType(ver), null);
                    addLink(link);
                }
            }
        }

        // adding the metadata link
        String metadataURL =
                buildURL(
                        info.getBaseURL(),
                        "ogc/styles/" + ResponseUtils.urlEncode(id) + "/metadata",
                        null,
                        URLMangler.URLType.SERVICE);
        Link link =
                new Link(
                        metadataURL,
                        "describeBy",
                        MediaType.APPLICATION_JSON_VALUE,
                        "The style metadata");
        addLink(link);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
