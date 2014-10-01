/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.catalog;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.logging.Logging;

import java.util.logging.Logger;

/**
 * Cascade deletes the visited objects, and modifies related object
 * so that they are still consistent.
 * In particular:
 * <ul>
 * <li>When removing a {@link LayerInfo} the {@link LayerGroupInfo} are modified
 * by removing the layer. If the layer was the last one, the layer group
 * is removed as well.
 * </li>
 * <li>When a {@link StyleInfo} is removed the layers using it as the default
 * style are set with the default style, the layers that use is as an extra
 * style are modified by removing it. Also, the layer groups using it
 * are changed so that the default layer style is used in place of the
 * one being removed
 * </li>
 */
public class TJSCascadeDeleteVisitor implements TJSCatalogVisitor {

    static final Logger LOGGER = Logging.getLogger(TJSCascadeDeleteVisitor.class);
    TJSCatalog catalog;

    public TJSCascadeDeleteVisitor(TJSCatalog catalog) {
        this.catalog = catalog;
    }

    public void visit(TJSCatalog catalog) {
    }

    public void visit(FrameworkInfo framework) {
        //borrar los datasets que dependen de este framework
        //Alvaro Javier Fuentes Suarez, 11:38 p.m. 1/8/13
        for (DatasetInfo s : catalog.getDatasetsByFramework(framework.getId())) {
            s.accept(this);
        }

        //TODO Borrar las capas asociadas con este framework por un JoinData!!!!!, Alvaro Javier

        //ahora borrar el framework
        //Alvaro Javier Fuentes Suarez, 11:39 p.m. 1/8/13
        LOGGER.info("Eliminando el framework " + framework.getName());
        catalog.remove(framework);
    }

    public void visit(DatasetInfo dataset) {
        //TODO Borrar las capas asociadas con este dataset por un JoinData!!!!!, Alvaro Javier

        //eliminar el dataset, Alvaro Javier Fuentes Suarez
        LOGGER.info("Eliminando el dataset " + dataset.getName());
        catalog.remove(dataset);
    }

    @Override
    public void visit(JoinedMapInfo joinedMap) {
        //nada por ahora, Alvaro Javier
    }

    public void visit(TJSCatalogObject object) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void visit(DataStoreInfo dataStore) {
        //borrar los datasets que dependen de este data store
        //Alvaro Javier Fuentes Suarez, 11:39 p.m. 1/8/13
        for (DatasetInfo s : catalog.getDatasets(dataStore.getId())) {
            s.accept(this);
        }
        //ahora borrar el datastore
        //Alvaro Javier Fuentes Suarez, 11:39 p.m. 1/8/13
        LOGGER.info("Eliminando el datastore " + dataStore.getName());
        catalog.remove(dataStore);
    }
}
