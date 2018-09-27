package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Arrays;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geowebcache.config.DefaultGridsets;

@JacksonXmlRootElement(localName = "TilingSchemes")
public class TilingSchemesDocument {

    private final GeoServer geoServer;
    private final DefaultGridsets gridSets;
    private List<String> schemesNames;

    public TilingSchemesDocument(GeoServer geoServer, DefaultGridsets gridSets) {
        this.geoServer = geoServer;
        this.gridSets = gridSets;
        schemesNames =
                Arrays.asList(
                        gridSets.worldEpsg4326().getName(), gridSets.worldEpsg3857().getName());
    }

    @JacksonXmlProperty(localName = "TilingSchemes")
    public List<String> getTilingSchemes() {
        return schemesNames;
    }

    public void setTilingSchemes(List<String> schemesNames) {
        this.schemesNames = schemesNames;
    }
}
