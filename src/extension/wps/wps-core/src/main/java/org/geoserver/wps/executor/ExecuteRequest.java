/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Ows11Util;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/**
 * Centralizes some common request parsing activities
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ExecuteRequest {

    ExecuteType request;

    public ExecuteRequest(ExecuteType request) {
        this.request = request;
    }

    /**
     * The wrapped WPS 1.0 request
     * 
     * @return
     */
    public ExecuteType getRequest() {
        return request;
    }

    /**
     * True if the request is asynchronous
     * 
     * @return
     */
    public boolean isAsynchronous() {
        return request.getResponseForm() != null
                && request.getResponseForm().getResponseDocument() != null
                && request.getResponseForm().getResponseDocument().isStoreExecuteResponse();
    }

    /**
     * Returns true if status update is requested
     * 
     * @return
     */
    public boolean isStatusEnabled() {
        return isAsynchronous() && request.getResponseForm().getResponseDocument().isStatus();
    }

    /**
     * Returns the process name according to the GeoTools API
     * 
     * @return
     */
    public Name getProcessName() {
        return Ows11Util.name(request.getIdentifier());
    }

    /**
     * Returns the process inputs according to the GeoTools API expectations
     * 
     * @param request
     * @return
     */
    public LazyInputMap getProcessInputs(WPSExecutionManager manager) {
        // get the input descriptors
        Name processName = Ows11Util.name(request.getIdentifier());
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName);
        if(pf == null) {
            throw new WPSException("Unknown process " + processName);
        }
        
        final Map<String, Parameter<?>> parameters = pf.getParameterInfo(processName);

        // turn them into a map of input providers
        Map<String, InputProvider> providers = new HashMap<String, InputProvider>();
        for (Iterator i = request.getDataInputs().getInput().iterator(); i.hasNext();) {
            InputType input = (InputType) i.next();
            String inputId = input.getIdentifier().getValue();

            // locate the parameter for this request
            Parameter p = parameters.get(inputId);
            if (p == null) {
                throw new WPSException("No such parameter: " + inputId);
            }

            // find the ppio
            String mime = null;
            if (input.getData() != null && input.getData().getComplexData() != null) {
                mime = input.getData().getComplexData().getMimeType();
            } else if (input.getReference() != null) {
                mime = input.getReference().getMimeType();
            }
            ProcessParameterIO ppio = ProcessParameterIO.find(p, manager.applicationContext, mime);
            if (ppio == null) {
                throw new WPSException("Unable to decode input: " + inputId);
            }

            // build the provider
            InputProvider provider = new SimpleInputProvider(input, ppio, manager,
                    manager.applicationContext);

            // store the input
            if (p.maxOccurs > 1) {
                ListInputProvider lp = (ListInputProvider) providers.get(p.key);
                if (lp == null) {
                    lp = new ListInputProvider(provider);
                    providers.put(p.key, lp);
                } else {
                    lp.add(provider);
                }
            } else {
                providers.put(p.key, provider);
            }
        }

        return new LazyInputMap(providers);
    }

    public boolean isLineageRequested() {
        return request.getResponseForm() != null
                && request.getResponseForm().getResponseDocument() != null
                && request.getResponseForm().getResponseDocument().isLineage();
    }

    /**
     * Returns null if nothing specific was requested, the list otherwise
     * @return
     */
    public List<DocumentOutputDefinitionType> getRequestedOutputs() {
        // in case nothing specific was requested
        if (request.getResponseForm() == null
                || request.getResponseForm().getResponseDocument() == null
                || request.getResponseForm().getResponseDocument().getOutput() == null) {
            return null;
        }
        
        List<DocumentOutputDefinitionType> result = new ArrayList<DocumentOutputDefinitionType>();
        EList outputs = request.getResponseForm().getResponseDocument().getOutput();
        for (Object output : outputs) {
            result.add((DocumentOutputDefinitionType) output);
        }
        return result;
    }

}
