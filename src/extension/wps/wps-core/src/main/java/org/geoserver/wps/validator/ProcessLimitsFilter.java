/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.DelegatingProcessFactory;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.process.ProcessFilter;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.util.Range;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Validator;

/**
 * A process filter that applies the well known input validators to the process parameters, so that
 * DescribeFeatureType can advertise them to the world
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessLimitsFilter
        implements ProcessFilter, ApplicationContextAware, ExtensionPriority {

    static final Logger LOGGER = Logging.getLogger(ProcessLimitsFilter.class);

    static final Multimap<String, WPSInputValidator> EMPTY_MULTIMAP = ImmutableMultimap.of();

    /** Key where the parameter validators will be stored */
    public static final String VALIDATORS_KEY = "wpsValidators";

    GeoServer geoServer;

    private ApplicationContext applicationContext;

    public ProcessLimitsFilter(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        return new ProcessLimitFactory(pf);
    }

    class ProcessLimitFactory extends DelegatingProcessFactory {

        public ProcessLimitFactory(ProcessFactory delegate) {
            super(delegate);
        }

        @Override
        public Map<String, Parameter<?>> getParameterInfo(Name name) {
            // look for the ProcessInfo for this process, it might have restrictions
            WPSInfo wps = geoServer.getService(WPSInfo.class);
            ProcessFactory factory = GeoServerProcessors.createProcessFactory(name, false);
            ProcessInfo processInfo = null;
            for (ProcessGroupInfo group : wps.getProcessGroups()) {
                if (group.getFactoryClass().equals(factory.getClass())) {
                    List<ProcessInfo> filteredProcesses = group.getFilteredProcesses();
                    for (ProcessInfo pi : filteredProcesses) {
                        if (name.equals(pi.getName())) {
                            processInfo = pi;
                            break;
                        }
                    }
                    if (processInfo != null) {
                        break;
                    }
                }
            }

            // get the parameters, see if we have any restriction to apply
            Map<String, Parameter<?>> result = super.getParameterInfo(name);
            int maxComplexInputSize = wps.getMaxComplexInputSize();
            if (maxComplexInputSize <= 0
                    && (processInfo == null
                            || processInfo.getValidators() == null
                            || processInfo.getValidators().isEmpty())) {
                return result;
            } else {
                Multimap<String, WPSInputValidator> validatorsMap =
                        processInfo != null ? processInfo.getValidators() : EMPTY_MULTIMAP;
                // clone just to be on the safe side
                HashMap<String, Parameter<?>> params = new LinkedHashMap<>(result);
                for (String paramName : params.keySet()) {
                    Parameter<?> param = params.get(paramName);
                    Collection<WPSInputValidator> validators = validatorsMap.get(paramName);
                    // can we skip to build a clone?
                    if (validators == null
                            && (maxComplexInputSize <= 0
                                    || !ProcessParameterIO.isComplex(param, applicationContext))) {
                        continue;
                    }

                    // setup the global size limits. non complex params will just ignore it
                    Map<String, Object> metadataClone = new HashMap(param.metadata);
                    if (wps.getMaxComplexInputSize() > 0) {
                        metadataClone.put(
                                MaxSizeValidator.PARAMETER_KEY, wps.getMaxComplexInputSize());
                    }

                    // collect all validator overrides
                    int maxOccurs = param.getMaxOccurs();
                    if (validators != null) {
                        metadataClone.put(VALIDATORS_KEY, validators);
                        for (Validator validator : validators) {
                            if (validator instanceof MaxSizeValidator) {
                                MaxSizeValidator msv = (MaxSizeValidator) validator;
                                int maxSizeMB = msv.getMaxSizeMB();
                                metadataClone.put(MaxSizeValidator.PARAMETER_KEY, maxSizeMB);
                            } else if (validator instanceof NumberRangeValidator) {
                                NumberRangeValidator rv = (NumberRangeValidator) validator;
                                Range<?> range = rv.getRange();
                                Comparable min = (Comparable) param.metadata.get(Parameter.MIN);
                                Comparable max = (Comparable) param.metadata.get(Parameter.MAX);
                                boolean restricting = false;
                                if (range.getMinValue() != null
                                        && (min == null
                                                || min.compareTo(range.getMinValue()) < 0)) {
                                    min = range.getMinValue();
                                    restricting = true;
                                }
                                if (range.getMaxValue() != null
                                        && (max == null
                                                || max.compareTo(range.getMaxValue()) > 0)) {
                                    max = range.getMaxValue();
                                    restricting = true;
                                }
                                if (restricting) {
                                    if (min != null) {
                                        metadataClone.put(Parameter.MIN, min);
                                    }
                                    if (max != null) {
                                        metadataClone.put(Parameter.MAX, max);
                                    }
                                }
                            } else if (validator instanceof MultiplicityValidator) {
                                MultiplicityValidator mv = (MultiplicityValidator) validator;
                                int max = mv.getMaxInstances();
                                if (max < maxOccurs) {
                                    maxOccurs = max;
                                }
                            }
                        }
                    }

                    // rebuild the param and put it in the params
                    Parameter<?> substitute =
                            new Parameter(
                                    param.key,
                                    param.type,
                                    param.title,
                                    param.description,
                                    param.required,
                                    param.minOccurs,
                                    maxOccurs,
                                    param.sample,
                                    metadataClone);
                    params.put(param.key, substitute);
                }

                return params;
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
