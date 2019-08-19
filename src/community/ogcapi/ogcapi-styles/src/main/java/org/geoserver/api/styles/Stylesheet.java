package org.geoserver.api.styles;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.util.Version;

@JsonInclude(NON_NULL)
@JsonPropertyOrder({"title", "version", "specification", "native", "tilingScheme", "link"})
public class Stylesheet extends AbstractDocument implements Serializable {

    private String title;
    private String version;
    private String specification;
    private boolean nativ;
    private String tilingScheme;
    private Link link;

    public Stylesheet(StyleInfo info, StyleHandler handler, Version version)
            throws MalformedURLException {

        this.title = "Stylesheet as " + handler.getName() + " " + version;
        this.version = version.toString();
        this.specification =
                Optional.ofNullable(handler.getSpecification(version))
                        .map(URL::toExternalForm)
                        .orElse(null);
        this.nativ = isNative(info, handler, version);
        // no instanceof to avoid dependency, MBStyle is a plugin
        if (handler.getFormat().equals("MBStyle")) {
            this.tilingScheme = "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible";
        }
        String baseURL = APIRequestInfo.get().getBaseURL();
        String styleId = NCNameResourceCodec.encode(info);
        String mimeType = handler.mimeType(version);
        String url =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/styles/styles/" + ResponseUtils.urlEncode(styleId),
                        Collections.singletonMap("f", mimeType),
                        URLMangler.URLType.SERVICE);
        this.link = new Link(url, "stylesheet", mimeType, null);
    }

    private boolean isNative(StyleInfo info, StyleHandler handler, Version version) {
        if (info.getFormat() == null) {
            return handler instanceof SLDHandler && SLDHandler.VERSION_10.equals(version);
        }
        if (!info.getFormat().equals(handler.getFormat())) {
            return false;
        }
        return (info.getFormatVersion() == null && version.equals(handler.getVersions().get(0)))
                || (info.getFormatVersion() != null && info.getFormatVersion().equals(version));
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getSpecification() {
        return specification;
    }

    public boolean isNative() {
        return nativ;
    }

    public String getTilingScheme() {
        return tilingScheme;
    }

    public Link getLink() {
        return link;
    }
}
