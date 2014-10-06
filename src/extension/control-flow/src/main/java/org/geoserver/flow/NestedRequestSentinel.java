/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

/**
 * Helper class that can be used to handle nested request callbacks, as the ones generated
 * by WMS GWC integration, so that a callback can take different actions based on the
 * nesting level of the current request (normally it will want to act only on the outermost
 * request)
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class NestedRequestSentinel {

    ThreadLocal<Integer> NESTING_LEVEL = new ThreadLocal<Integer>();
    
    /**
     * Call this method any time a request starts 
     */
    public void start() {
        Integer nesting = NESTING_LEVEL.get();
        if(nesting == null) {
            nesting = 1;
        } else {
            nesting++;
        }
        NESTING_LEVEL.set(nesting);
    }
    
    /**
     * Call this method any time a request ends (at the end of the processing) 
     */
    public void stop() {
        Integer nesting = NESTING_LEVEL.get();
        if(nesting != null) {
            nesting--;
            if(nesting == 0) {
                NESTING_LEVEL.remove();
            } else {
                NESTING_LEVEL.set(nesting);
            }
        }
    }
    
    /**
     * Returns false if start() has been called two or more times than end()
     * @return
     */
    public boolean isOutermostRequest() {
        Integer nesting = NESTING_LEVEL.get();
        return nesting == null || nesting < 2;
    }
}
