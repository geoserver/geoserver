/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.util.concurrent.BlockingQueue;
import org.geotools.renderer.lite.StreamingRenderer;

/**
 * A {@link StreamingRenderer} subclass that will only paint vector shapes
 *
 * @author Andrea Aime - GeoSolutions
 */
class PureVectorRenderer extends StreamingRenderer {

    @Override
    protected BlockingQueue<RenderingRequest> getRequestsQueue() {
        return new PureVectorRenderingBlockingQueue(10000);
    }

    protected class PureVectorRenderingBlockingQueue
            extends StreamingRenderer.RenderingBlockingQueue {
        private static final long serialVersionUID = -1769980899098830523L;

        public PureVectorRenderingBlockingQueue(int capacity) {
            super(capacity);
        }

        @Override
        public boolean add(RenderingRequest e) {
            // only really accepts vector paint requests, UTFGrid makes no sense with the other
            // types
            // of requests
            if (e instanceof EndRequest || e instanceof PaintShapeRequest) {
                return super.add(e);
            } else {
                return true;
            }
        }
    }
}
