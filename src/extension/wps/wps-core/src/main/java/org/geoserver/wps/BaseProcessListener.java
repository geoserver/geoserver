package org.geoserver.wps;

/**
 * A base process listener that does nothing, subclasses can extend to only implement the methods
 * they are interested into
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class BaseProcessListener implements ProcessListener {

    @Override
    public void submitted(ProcessEvent event) throws WPSException {

    }

    @Override
    public void progress(ProcessEvent event) throws WPSException {

    }

    @Override
    public void completed(ProcessEvent event) throws WPSException {

    }

    @Override
    public void cancelled(ProcessEvent event) throws WPSException {

    }

    @Override
    public void failed(ProcessEvent event) {

    }


}
