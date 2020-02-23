/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

@SuppressWarnings("serial")
public class SQLViewAttributeProvider extends GeoServerDataProvider<SQLViewAttribute> {

    static final Logger LOGGER = Logging.getLogger(SQLViewAttribute.class);

    static final Property<SQLViewAttribute> NAME =
            new BeanProperty<SQLViewAttribute>("name", "name");

    static final Property<SQLViewAttribute> TYPE =
            new AbstractProperty<SQLViewAttribute>("type") {

                public Object getPropertyValue(SQLViewAttribute item) {
                    if (item.getType() != null) {
                        return item.getType().getSimpleName();
                    }
                    return null;
                }
            };

    static final Property<SQLViewAttribute> SRID =
            new BeanProperty<SQLViewAttribute>("srid", "srid");

    static final Property<SQLViewAttribute> PK = new BeanProperty<SQLViewAttribute>("pk", "pk");

    List<SQLViewAttribute> attributes = new ArrayList<SQLViewAttribute>();

    public SQLViewAttributeProvider() {
        setEditable(true);
    }

    void setFeatureType(SimpleFeatureType ft, VirtualTable vt) {
        attributes.clear();
        for (AttributeDescriptor ad : ft.getAttributeDescriptors()) {
            SQLViewAttribute at =
                    new SQLViewAttribute(ad.getLocalName(), ad.getType().getBinding());
            String attName = ad.getName().getLocalPart();
            attributes.add(at);
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                if (gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID) != null) {
                    at.setSrid((Integer) gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID));
                } else if (gd.getCoordinateReferenceSystem() != null) {
                    try {
                        at.setSrid(CRS.lookupEpsgCode(gd.getCoordinateReferenceSystem(), false));
                    } catch (Exception e) {
                        // it is ok, we're just trying to facilitate the user's life here
                    }
                }
                if (vt != null && vt.getGeometries().contains(attName)) {
                    at.setSrid(vt.getNativeSrid(attName));
                    at.setType(vt.getGeometryType(attName));
                }
            }
            if (vt != null
                    && vt.getPrimaryKeyColumns() != null
                    && vt.getPrimaryKeyColumns().contains(attName)) {
                at.setPk(true);
            }
        }
    }

    @Override
    protected List<SQLViewAttribute> getItems() {
        return attributes;
    }

    @Override
    protected List<Property<SQLViewAttribute>> getProperties() {
        return Arrays.asList(NAME, TYPE, SRID, PK);
    }

    /** Sets the geometries details and the primary key columns into the virtual table */
    @SuppressWarnings("unchecked")
    public void fillVirtualTable(VirtualTable vt) {
        List<String> pks = new ArrayList<String>();
        for (SQLViewAttribute att : attributes) {
            if (Geometry.class.isAssignableFrom(att.getType())) {
                if (att.getSrid() == null) {
                    vt.addGeometryMetadatata(
                            att.getName(), (Class<? extends Geometry>) att.getType(), 4326);
                } else {
                    vt.addGeometryMetadatata(
                            att.getName(),
                            (Class<? extends Geometry>) att.getType(),
                            att.getSrid());
                }
            }
            if (att.pk) {
                pks.add(att.getName());
            }
        }
        if (pks.size() > 0) {
            vt.setPrimaryKeyColumns(pks);
        }
    }
}
