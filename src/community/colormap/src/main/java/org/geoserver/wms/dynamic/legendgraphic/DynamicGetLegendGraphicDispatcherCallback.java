package org.geoserver.wms.dynamic.legendgraphic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geotools.process.function.ProcessFunction;
import org.geotools.process.raster.DynamicColorMapProcess;
import org.geotools.process.raster.FilterFunction_svgColorMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

/**
 * A {@link DispatcherCallback} which intercepts a getLegendGraphicRequest and check whether that request involve a dynamicColorRamp rendering
 * transformation. In that case, it setup a legend based on the dynamic values coming from the request.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class DynamicGetLegendGraphicDispatcherCallback implements DispatcherCallback {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(DynamicGetLegendGraphicDispatcherCallback.class.getPackage().getName());

    private DynamicColorMapBuilder dynamicColorMapBuilder;

    public DynamicGetLegendGraphicDispatcherCallback(DynamicColorMapBuilder dynamicColorMapBuilder) {
        this.dynamicColorMapBuilder = dynamicColorMapBuilder;
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        final String id = operation.getId();

        // only intercepting getLegendGraphic invokation
        if (id.equalsIgnoreCase("getLegendGraphic")) {
            final Object[] params = operation.getParameters();
            if (params != null && params.length > 0 && params[0] instanceof GetLegendGraphicRequest) {

                final GetLegendGraphicRequest getLegendRequest = (GetLegendGraphicRequest) params[0];
                final List<Style> styles = getLegendRequest.getStyles();

                // Retrieving the colorRamp definition
                final String colorRamp = getColorRampDefinition(styles);
                Map map = request.getRawKvp();
                if (colorRamp != null && map.containsKey("LAYER")) {

                    Method method;
                    try {
                        Service service = new Service(operation.getService().getId(),
                                dynamicColorMapBuilder, null, null);
                        method = DynamicColorMapBuilder.class.getMethod("execute", new Class[] {
                                GetLegendGraphicRequest.class, Map.class, String.class });
                        Operation op = new Operation(id, service, method, new Object[] {
                                getLegendRequest, map, colorRamp });
                        return op;
                    } catch (SecurityException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Unable to prepare an operation for Dynamic "
                                            + "GetLegendGraphic due to the reported error. Proceeding with default operation");
                        }
                        return operation;
                    } catch (NoSuchMethodException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Unable to prepare an operation for Dynamic "
                                            + "GetLegendGraphic due to the reported error. Proceeding with default operation");
                        }
                        return operation;

                    }
                }
            }
        }
        return operation;
    }

    /**
     * Look for a ColorRamp string definition used by a {@link FilterFunction_svgColorMap} if any.
     * 
     * @param styles
     * @return
     */
    private String getColorRampDefinition(final List<Style> styles) {
        // Parsing the first style
        if (styles != null && styles.size() > 0) {
            final Style style = styles.get(0);
            final FeatureTypeStyle[] featureTypeStyles = style.featureTypeStyles().toArray(
                    new FeatureTypeStyle[0]);
            for (FeatureTypeStyle featureTypeStyle : featureTypeStyles) {

                // Getting the main transformation
                Expression transformation = featureTypeStyle.getTransformation();
                if (transformation instanceof ProcessFunction) {
                    final ProcessFunction processFunction = (ProcessFunction) transformation;
                    final String processName = processFunction.getName();

                    // Checking whether the processFunction is a DynamicColorMapProcess
                    if (processName.equalsIgnoreCase(DynamicColorMapProcess.NAME)) {
                        return getColorRampDefinition(processFunction);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Look for a ColorRamp definition used by a {@link DynamicColorMapProcess} rendering transformation.
     * 
     * @param processFunction
     * @return
     */
    private String getColorRampDefinition(final ProcessFunction processFunction) {
        List<Expression> functionParameters = processFunction.getParameters();
        if (functionParameters.size() == 2) { // 1 is the data, 2 is the colormap
            Expression param = functionParameters.get(1);
            if (param instanceof Function) {
                Function paramFunction = (Function) param;
                List<Expression> functionParams = paramFunction.getParameters();
                if (functionParams.size() == 2) {

                    // Getting the second parameter representing the colorRamp
                    param = functionParams.get(1);
                    if (param instanceof FilterFunction_svgColorMap) {
                        FilterFunction_svgColorMap colorMap = (FilterFunction_svgColorMap) param;
                        List<Expression> colorMapParams = colorMap.getParameters();
                        if (colorMapParams != null && colorMapParams.size() == 3) {
                            return colorMapParams.get(0).toString();
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {
    }
}
