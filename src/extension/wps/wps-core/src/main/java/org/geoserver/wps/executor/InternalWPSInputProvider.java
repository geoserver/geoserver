/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Map;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.kvp.ExecuteKvpRequestReader;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.feature.FeatureCollection;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Handles an chaining call to another WPS process
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InternalWPSInputProvider extends AbstractInputProvider {

    private WPSExecutionManager executor;

    private ExecuteRequest executeRequest;

    private int longSteps;

    public InternalWPSInputProvider(
            InputType input,
            ProcessParameterIO ppio,
            WPSExecutionManager executor,
            ApplicationContext context)
            throws Exception {
        super(input, ppio);
        this.executor = executor;

        ExecuteType request = null;
        InputReferenceType ref = input.getReference();
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            request = (ExecuteType) ref.getBody();
        } else {
            ExecuteKvpRequestReader reader =
                    (ExecuteKvpRequestReader) context.getBean("executeKvpRequestReader");
            request = (ExecuteType) kvpParse(ref.getHref(), reader);
        }
        executeRequest = new ExecuteRequest(request);
        LazyInputMap inputs = executeRequest.getProcessInputs(executor);
        this.longSteps = inputs.longStepCount() + 1;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        Map<String, Object> results = executor.submitChained(executeRequest, listener);

        // Gets the suitable result item searching by name/data-type, otherwise it returns the first
        // result item.
        if (executeRequest.getRequest().getResponseForm() != null) {
            net.opengis.wps10.ResponseFormType responseForm =
                    executeRequest.getRequest().getResponseForm();
            net.opengis.wps10.ResponseDocumentType responseDoc = responseForm.getResponseDocument();

            if (responseDoc != null) {
                for (Object output : responseDoc.getOutput()) {
                    net.opengis.wps10.DocumentOutputDefinitionType outputType =
                            (net.opengis.wps10.DocumentOutputDefinitionType) output;
                    String parameterName = outputType.getIdentifier().getValue();

                    for (Map.Entry<String, Object> entry : results.entrySet()) {

                        if (entry.getKey().equalsIgnoreCase(parameterName)) {
                            Object value = entry.getValue();
                            if (value != null && ppio.getType().isInstance(value)) return value;
                        }
                    }
                }
            }
        }

        Object obj = results.values().iterator().next();
        if (obj != null && !ppio.getType().isInstance(obj)) {
            throw new WPSException(
                    "The process output is incompatible with the input target type, was expecting "
                            + ppio.getType().getName()
                            + " and got "
                            + obj.getClass().getName());
        }

        // make sure we have the process receiving this fail if cancellation triggers
        if (obj instanceof FeatureCollection) {
            obj = CancellingFeatureCollectionBuilder.wrap((FeatureCollection) obj, listener);
        }

        return obj;
    }

    @Override
    public int longStepCount() {
        return longSteps;
    }
}
