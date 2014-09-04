/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Represents the response to the GetCentralRevision call
 * @author aaime
 */
public class CentralRevisionsType {

    public static class LayerRevision {
        QName typeName;

        long centralRevision;

        public LayerRevision(QName typeName, long centralRevision) {
            if (typeName == null) {
                throw new IllegalArgumentException("Layer revision type name cannot be null");
            }
            this.typeName = typeName;
            this.centralRevision = centralRevision;
        }

        public QName getTypeName() {
            return typeName;
        }

        public long getCentralRevision() {
            return centralRevision;
        }
        
        
    }

    List<LayerRevision> layerRevisions = new ArrayList<LayerRevision>();

    public List<LayerRevision> getLayerRevisions() {
        return layerRevisions;
    }

}
