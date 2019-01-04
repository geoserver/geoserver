/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A freemarker model that's at the same time a sequence and a map */
public class SequenceMapModel extends MapModel implements TemplateSequenceModel {
    private ArrayList list;

    public SequenceMapModel(Map map, BeansWrapper wrapper) {
        super(map, wrapper);

        this.list = new ArrayList(map.values());
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return wrap(list.get(index));
    }

    protected Set keySet() {
        // override, just return the map contents
        Set set = new HashSet();
        set.addAll(((Map) object).keySet());

        return set;
    }
}
