/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * A base process listener that does nothing, subclasses can extend to only implement the methods
 * they are interested into
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessListenerAdapter implements ProcessListener {

    @Override
    public void submitted(ProcessEvent event) throws WPSException {}

    @Override
    public void progress(ProcessEvent event) throws WPSException {}

    @Override
    public void succeeded(ProcessEvent event) throws WPSException {}

    @Override
    public void dismissing(ProcessEvent event) throws WPSException {}

    @Override
    public void dismissed(ProcessEvent event) throws WPSException {}

    @Override
    public void failed(ProcessEvent event) {}
}
