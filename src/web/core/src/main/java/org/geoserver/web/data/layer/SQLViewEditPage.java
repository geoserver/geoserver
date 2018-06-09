/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.jdbc.VirtualTable;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Allows editing a SQL view and then going back to
 *
 * @author Andrea Aime - OpenGeo
 */
public class SQLViewEditPage extends SQLViewAbstractPage {
    /** serialVersionUID */
    private static final long serialVersionUID = 7301602944709110330L;

    ResourceConfigurationPage previusPage;
    String originalName;
    FeatureTypeInfo tinfo;

    public SQLViewEditPage(FeatureTypeInfo type, ResourceConfigurationPage previousPage)
            throws IOException {
        super(
                type.getStore().getWorkspace().getName(),
                type.getStore().getName(),
                type.getName(),
                type.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class));
        VirtualTable vt =
                type.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class);
        tinfo = type;
        originalName = vt.getName();
        this.previusPage = previousPage;
    }

    @Override
    protected void onSave() {
        try {
            VirtualTable vt = buildVirtualTable();
            SimpleFeatureType rawFeatureType = getFeatureType(vt);

            // update the feature type info
            tinfo.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
            CoordinateReferenceSystem crs = rawFeatureType.getCoordinateReferenceSystem();
            if (crs != null) {
                tinfo.setNativeCRS(crs);
            }
            tinfo.setNativeName(vt.getName());

            // set it back in the main page and redirect to it
            previusPage.updateResource(tinfo);
            setResponsePage(previusPage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(
                    new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                            .getString());
        }
    }

    protected void onCancel() {
        setResponsePage(previusPage);
    }
}
