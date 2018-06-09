package org.geoserver.wfs;

import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.wfs.request.GetCapabilitiesRequest;

/**
 * This interface is essentially an alias for the Generic ExtendedCapabilitiesProvider so that the
 * Type Parameters do not need to be declared everywhere and so that when loading extensions there
 * is a distinct class of beans to load
 *
 * @author Jesse Eichar, camptocamp
 */
public interface WFSExtendedCapabilitiesProvider
        extends ExtendedCapabilitiesProvider<WFSInfo, GetCapabilitiesRequest> {}
