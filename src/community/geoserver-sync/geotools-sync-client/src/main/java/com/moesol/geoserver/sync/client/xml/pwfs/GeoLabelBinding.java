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

package com.moesol.geoserver.sync.client.xml.pwfs;




import java.math.BigDecimal;

import javax.xml.namespace.QName;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import com.moesol.geoserver.sync.client.xml.pwfs.GeoLabel.XAnchorType;
import com.moesol.geoserver.sync.client.xml.pwfs.GeoLabel.YAnchorType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class GeoLabelBinding extends AbstractComplexBinding {
    public static final String NS_PWFS = "http://www.polexis.com/pwfs";
    public static final QName QN_GEO_LABEL_TYPE = new QName(NS_PWFS, "GeoLabelType");

    private final GeometryFactory gf;

    public GeoLabelBinding(GeometryFactory gf) {
        this.gf = gf;
    }

    @Override
    public QName getTarget() {
        return QN_GEO_LABEL_TYPE;
    }

    @Override
    public Class<?> getType() {
        return GeoLabel.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        CoordinateSequence seq = new CoordinateArraySequence(new Coordinate[]{(Coordinate) node.getChildValue(Coordinate.class)});
        GeoLabel label = new GeoLabel(seq, gf);
        label.setRotation(((BigDecimal)node.getChildValue("rotation")).doubleValue());
        label.setText((String) node.getChildValue("text"));
        label.setxAnchor(XAnchorType.valueOf(node.getChildValue("xAnchor").toString().toUpperCase()));
        label.setyAnchor(YAnchorType.valueOf(node.getChildValue("yAnchor").toString().toUpperCase()));
        return label;
    }

}
