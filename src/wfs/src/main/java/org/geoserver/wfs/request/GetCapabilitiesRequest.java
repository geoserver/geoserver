/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.Arrays;
import java.util.List;
import net.opengis.ows10.Ows10Factory;
import net.opengis.ows11.Ows11Factory;
import org.eclipse.emf.ecore.EObject;

/**
 * WFS GetCapabilities request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GetCapabilitiesRequest extends RequestObject {

    private String[] acceptLanguages;

    public static GetCapabilitiesRequest adapt(Object request) {
        if (request instanceof EObject type1) {
            return new WFS11(type1);
        } else if (request instanceof EObject type) {
            return new WFS20(type);
        }
        return null;
    }

    protected GetCapabilitiesRequest(EObject adaptee) {
        super(adaptee);
    }

    @SuppressWarnings("unchecked")
    public List<String> getSections() {
        return eGet(adaptee, "sections.section", List.class);
    }

    public String getUpdateSequence() {
        return eGet(adaptee, "updateSequence", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getAcceptVersions() {
        return eGet(adaptee, "acceptVersions.version", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getAcceptFormats() {
        return eGet(adaptee, "acceptFormats.outputFormat", List.class);
    }

    public void setAcceptVersions(String... versions) {
        Object acceptedVersions = createAcceptedVersions();
        eAdd(acceptedVersions, "version", Arrays.asList(versions));
        eSet(adaptee, "acceptVersions", acceptedVersions);
    }

    public String getNamespace() {
        return eGet(adaptee, "namespace", String.class);
    }

    public void setNamespace(String namespace) {
        eSet(adaptee, "namespace", namespace);
    }

    protected abstract Object createAcceptedVersions();

    public static class WFS11 extends GetCapabilitiesRequest {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        protected Object createAcceptedVersions() {
            return Ows10Factory.eINSTANCE.createAcceptVersionsType();
        }
    }

    public static class WFS20 extends GetCapabilitiesRequest {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        protected Object createAcceptedVersions() {
            return Ows11Factory.eINSTANCE.createAcceptVersionsType();
        }
    }

    public String[] getAcceptLanguages() {
        return acceptLanguages;
    }

    public void setAcceptLanguages(String[] acceptLanguages) {
        this.acceptLanguages = acceptLanguages;
    }
}
