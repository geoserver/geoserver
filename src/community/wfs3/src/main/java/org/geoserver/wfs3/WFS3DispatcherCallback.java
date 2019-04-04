/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs3.response.OpenAPIResponse;
import org.geoserver.wfs3.response.RFCGeoJSONFeaturesResponse;
import org.springframework.http.HttpHeaders;

public class WFS3DispatcherCallback extends AbstractDispatcherCallback {

    private Lazy<Service> wfs3 = new Lazy<>();
    private Lazy<Service> fallback = new Lazy<>();

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Service wfs3 =
                this.wfs3.getOrCompute(() -> (Service) GeoServerExtensions.bean("wfsService-3.0"));
        Service fallback =
                this.fallback.getOrCompute(
                        () -> (Service) GeoServerExtensions.bean("wfsService-2.0"));
        if (wfs3.equals(service) && "GetCapabilities".equals(request.getRequest())) {
            request.setServiceDescriptor(fallback);
            return fallback;
        }
        return service;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        Service wfs3 =
                this.wfs3.getOrCompute(() -> (Service) GeoServerExtensions.bean("wfsService-3.0"));
        if (wfs3.equals(request.getServiceDescriptor())) {
            String header = request.getHttpRequest().getHeader(HttpHeaders.ACCEPT);
            Object parsedRequest = operation.getParameters()[0];
            Method formatSetter =
                    OwsUtils.setter(parsedRequest.getClass(), "outputFormat", String.class);
            Method formatGetter =
                    OwsUtils.getter(parsedRequest.getClass(), "outputFormat", String.class);
            try {
                // can we manipulate the format, and it's not already set?
                if (formatGetter != null
                        && formatSetter != null
                        && formatGetter.invoke(parsedRequest) == null) {

                    if (header != null && !"*/*".equalsIgnoreCase(header)) {
                        // figure out which format we want to use, take the fist supported one
                        LinkedHashSet<String> acceptedFormats =
                                new LinkedHashSet<>(Arrays.asList(header.split("\\s*,\\s*")));
                        List<String> availableFormats =
                                DefaultWebFeatureService30.getAvailableFormats(result.getClass());
                        acceptedFormats.retainAll(availableFormats);
                        if (!acceptedFormats.isEmpty()) {
                            String format = acceptedFormats.iterator().next();
                            setOutputFormat(request, parsedRequest, formatSetter, format);
                        }
                    } else {
                        // handle defaults if really nothing is specified
                        String defaultType = BaseRequest.JSON_MIME;
                        if ("getFeature".equals(request.getRequest())) {
                            defaultType = RFCGeoJSONFeaturesResponse.MIME;
                        } else if ("api".equals(request.getRequest())) {
                            defaultType = OpenAPIResponse.OPEN_API_MIME;
                        }
                        // for getStyle we're going to use the "native" format if possible
                        if (!"getStyle".equals(request.getRequest())) {
                            setOutputFormat(request, parsedRequest, formatSetter, defaultType);
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServiceException("Failed to handle Accept header", e);
            }
        }

        return super.operationExecuted(request, operation, result);
    }

    private void setOutputFormat(
            Request request, Object parsedRequest, Method formatSetter, String outputformat)
            throws IllegalAccessException, InvocationTargetException {
        request.setOutputFormat(outputformat);
        formatSetter.invoke(parsedRequest, outputformat);
    }

    /**
     * Thread safe lazy calculation, used to avoid bean circular dependencies
     *
     * @param <T>
     */
    public final class Lazy<T> {
        private volatile T value;

        public T getOrCompute(Supplier<T> supplier) {
            final T result = value; // Just one volatile read
            return result == null ? maybeCompute(supplier) : result;
        }

        private synchronized T maybeCompute(Supplier<T> supplier) {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
