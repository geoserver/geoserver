/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client.xml;




import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.feature.FeatureCollection;
import org.geotools.gml3.GML;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class ComplexFeatureCollectionTypeBinding extends AbstractComplexBinding {

    @Override
    public QName getTarget() {
        return GML.AbstractFeatureCollectionType;
    }

    @Override
    public Class getType() {
        return FeatureCollection.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        FeatureCollection coll = (FeatureCollection) node.getChildValue(FeatureCollection.class);
        List<Node> featureMembers = node.getChildren("featureMember");

        if(coll == null) {
            FeatureType type = null;
            if(featureMembers.size() > 0) {
                Feature f = (Feature) featureMembers.get(0).getChildValue(Feature.class);
                if(f != null) {
                    type = f.getType();
                }
            }
            coll = new ComplexFeatureCollection(null, type);
        }

        for(Node n : featureMembers) {
            ((List<Node>) coll).addAll(n.getChildValues(Feature.class));
            
        	//coll.addAll(n.getChildValues(Feature.class));
        }

        return coll;
    }
}
