/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerGroupInfo;

/** Simple detachable model listing all the available LayerGroup modes. */
public class LayerGroupModeModel extends LoadableDetachableModel<List<LayerGroupInfo.Mode>> {

    private static final long serialVersionUID = 1781202562325730121L;

    @Override
    protected List<LayerGroupInfo.Mode> load() {
        return new ArrayList<LayerGroupInfo.Mode>(Arrays.asList(LayerGroupInfo.Mode.values()));
    }
}
