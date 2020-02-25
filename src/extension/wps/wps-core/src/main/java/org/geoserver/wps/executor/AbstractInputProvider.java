/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.Validators;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Validator;

/**
 * Base class for single value input providers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractInputProvider implements InputProvider {

    /** Creates an input provider */
    public static InputProvider getInputProvider(
            InputType input,
            ProcessParameterIO ppio,
            WPSExecutionManager executor,
            ApplicationContext context,
            Collection<Validator> validators)
            throws Exception {
        InputProvider provider;
        if (input.getReference() != null) {
            // this is a reference
            InputReferenceType ref = input.getReference();

            // grab the location and method
            String href = ref.getHref();

            if (href.startsWith("http://geoserver/wfs")) {
                provider = new InternalWFSInputProvider(input, ppio, context);
            } else if (href.startsWith("http://geoserver/wcs")) {
                provider = new InternalWCSInputProvider(input, ppio, context);
            } else if (href.startsWith("http://geoserver/wps")) {
                provider = new InternalWPSInputProvider(input, ppio, executor, context);
            } else {
                int maxSizeMB = Validators.getMaxSizeMB(validators);
                validators = Validators.filterOutClasses(validators, MaxSizeValidator.class);
                provider =
                        new RemoteRequestInputProvider(
                                input,
                                (ComplexPPIO) ppio,
                                executor.getConnectionTimeout(),
                                maxSizeMB * 1024 * 1024);
            }
        } else {
            provider = new SimpleInputProvider(input, ppio);
        }

        // add validation if necessary
        return ValidatingInputProvider.wrap(provider, validators);
    }

    InputType input;

    ProcessParameterIO ppio;

    Object value;

    String inputId;

    public AbstractInputProvider(InputType input, ProcessParameterIO ppio) {
        this.input = input;
        this.ppio = ppio;
        this.inputId = input.getIdentifier().getValue();
    }

    public String getInputId() {
        return inputId;
    }

    @Override
    public boolean resolved() {
        return value != null;
    }

    @Override
    public final Object getValue(ProgressListener listener) throws Exception {
        if (value == null) {
            value = getValueInternal(listener);
        }

        return value;
    }

    /** Computes the value */
    protected abstract Object getValueInternal(ProgressListener listener) throws Exception;

    /** Simulates what the Dispatcher is doing when parsing a KVP request */
    protected Object kvpParse(String href, KvpRequestReader reader) throws Exception {
        Map original = new KvpMap(KvpUtils.parseQueryString(href));
        KvpUtils.normalize(original);
        Map parsed = new KvpMap(original);
        List<Throwable> errors = KvpUtils.parse(parsed);
        if (errors.size() > 0) {
            throw new WPSException("Failed to parse KVP request", errors.get(0));
        }

        // hack to allow wcs filters to work... we should really upgrade the WCS models instead...
        Request r = Dispatcher.REQUEST.get();
        if (r != null) {
            r.setKvp(new CaseInsensitiveMap(parsed));
        }

        return reader.read(reader.createRequest(), parsed, original);
    }

    /** Returns the version from the kvp request */
    protected String getVersion(String href) {
        return (String) new KvpMap(KvpUtils.parseQueryString(href)).get("VERSION");
    }
}
