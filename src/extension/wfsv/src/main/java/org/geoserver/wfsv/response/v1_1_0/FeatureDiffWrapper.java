/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import java.util.Arrays;
import java.util.HashMap;

import org.geoserver.template.FeatureWrapper;
import org.geotools.data.FeatureDiffReader;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;


/**
 * Extends the FeatureWrapper so that FeatureDiffReaders can be provided to the
 * template as well
 *
 * @author Andrea Aime - TOPP
 */
public class FeatureDiffWrapper extends FeatureWrapper {
    

    public TemplateModel wrap(Object object) throws TemplateModelException {
        if (object instanceof FeatureDiffReader[]) {
            HashMap map = new HashMap();
            map.put("queryDiffs",
                new SimpleSequence(Arrays.asList((FeatureDiffReader[]) object), this));

            return new SimpleHash(map);
        } else if (object instanceof FeatureDiffReader) {
            HashMap map = new HashMap();
            FeatureDiffReader reader = (FeatureDiffReader) object;
            map.put("differences", new CollectionModel(new FeatureDiffCollection(reader), this));
            map.put("fromVersion", reader.getFromVersion());
            map.put("toVersion", reader.getToVersion());
            map.put("typeName", reader.getSchema().getTypeName());

            return new SimpleHash(map);
        }

        return super.wrap(object);
    }
}
