/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.util.MetadataMapModel;

/**
 * Support class to handle the links metadata map in a more convenient way:
 *
 * <ul>
 *   <li>Built-in keys and type, isolating unchecked warnings issues
 *   <li>Built-in null handling and key removal
 *   <li>Ensures that original objects are not modified until full form is submitted (no proxy
 *       around the objects here)
 * </ul>
 */
@SuppressWarnings("unchecked")
class LinksMetadataMapModel extends MetadataMapModel<List<LinkInfo>> {
    public LinksMetadataMapModel(IModel model) {
        super(model, LinkInfo.LINKS_METADATA_KEY, List.class);
    }

    @Override
    public List<LinkInfo> getObject() {
        List<LinkInfo> links = super.getObject();
        if (links == null) return new ArrayList<>();
        return links.stream().map(l -> ((LinkInfoImpl) l).clone()).collect(Collectors.toList());
    }

    @Override
    public void setObject(List<LinkInfo> links) {
        if (links == null || links.isEmpty()) super.setObject(null);
        else super.setObject(links);
    }
}
