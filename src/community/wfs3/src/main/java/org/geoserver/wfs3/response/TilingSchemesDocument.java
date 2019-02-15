/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Arrays;
import java.util.List;
import org.geowebcache.config.DefaultGridsets;

/** The object representing the list of available tiling schemes */
@JacksonXmlRootElement(localName = "TilingSchemes")
public class TilingSchemesDocument {

    private List<String> schemesNames;

    public TilingSchemesDocument(DefaultGridsets gridSets) {
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
