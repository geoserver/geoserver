/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import org.geotools.data.DataAccessFactory.Param;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.FloatParameter;
import org.geotools.parameter.Parameter;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author root
 */
public abstract class AbstractTJSDataStoreFactory implements TJSDataStoreFactorySpi {

    /**
     * Default Implementation abuses the naming convention.
     * <p>
     * Will return <code>Foo</code> for
     * <code>org.geotools.data.foo.FooFactory</code>.
     * </p>
     *
     * @return return display name based on class name
     */
    public String getDisplayName() {
        String name = this.getClass().getName();

        name = name.substring(name.lastIndexOf('.'));
        if (name.endsWith("Factory")) {
            name = name.substring(0, name.length() - 7);
        } else if (name.endsWith("FactorySpi")) {
            name = name.substring(0, name.length() - 10);
        }
        return name;
    }

    /**
     * Default implementation verifies the Map against the Param information.
     * <p>
     * It will ensure that:
     * <ul>
     * <li>params is not null
     * <li>Everything is of the correct type (or upcovertable
     * to the correct type without Error)
     * <li>Required Parameters are present
     * </ul>
     * </p>
     * <p>
     * <p>
     * Why would you ever want to override this method?
     * If you want to check that a expected file exists and is a directory.
     * </p>
     * Overrride:
     * <pre><code>
     * public boolean canProcess( Map params ) {
     *     if( !super.canProcess( params ) ){
     *          return false; // was not in agreement with getParametersInfo
     *     }
     *     // example check
     *     File file = (File) DIRECTORY.lookup( params ); // DIRECTORY is a param
     *     return file.exists() && file.isDirectory();
     * }
     * </code></pre>
     *
     * @param params
     * @return true if params is in agreement with getParametersInfo, override for additional checks.
     */
    public boolean canProcess(Map params) {
        if (params == null) {
            return false;
        }
        Param arrayParameters[] = getParametersInfo();
        for (int i = 0; i < arrayParameters.length; i++) {
            Param param = arrayParameters[i];
            Object value;
            if (!params.containsKey(param.key)) {
                if (param.required) {
                    return false; // missing required key!
                } else {
                    continue;
                }
            }
            try {
                value = param.lookUp(params);
            } catch (IOException e) {
                // could not upconvert/parse to expected type!
                // even if this parameter is not required
                // we are going to refuse to process
                // these params
                return false;
            }
            if (value == null) {
                if (param.required) {
                    return (false);
                }
            } else {
                if (!param.type.isInstance(value)) {
                    return false; // value was not of the required type
                }
            }
        }
        return true;
    }

    /**
     * Defaults to true, only a few datastores need to check for drivers.
     *
     * @return <code>true</code>, override to check for drivers etc...
     */
    public boolean isAvailable() {
        return true;
    }

    public ParameterDescriptorGroup getParameters() {
        Param params[] = getParametersInfo();
        DefaultParameterDescriptor parameters[] = new DefaultParameterDescriptor[params.length];
        for (int i = 0; i < params.length; i++) {
            Param param = params[i];
            parameters[i] = new ParamDescriptor(params[i]);
        }
        Map properties = new HashMap();
        properties.put("name", getDisplayName());
        properties.put("remarks", getDescription());
        return new DefaultParameterDescriptorGroup(properties, parameters);
    }

    /**
     * Returns the implementation hints. The default implementation returns en empty map.
     */
    public Map<java.awt.RenderingHints.Key, ?> getImplementationHints() {
        return Collections.EMPTY_MAP;
    }

    public Map<String, Serializable> filterParamsForSave(Map params) {
        HashMap<String, Serializable> saveMap = new HashMap<String, Serializable>();
        for (Iterator i = params.keySet().iterator(); i.hasNext(); ) {
            String key = i.next().toString();
            Object o = params.get(key);
            if (o instanceof Serializable) {
                saveMap.put(key, (Serializable) o);
            }
        }
        return saveMap;
    }

}

class ParamDescriptor extends DefaultParameterDescriptor {
    private static final long serialVersionUID = 1L;
    Param param;

    public ParamDescriptor(Param param) {
        super(DefaultParameterDescriptor.create(param.key, param.description, param.type, param.sample, param.required));
        this.param = param;
    }

    public ParameterValue createValue() {
        if (Double.TYPE.equals(getValueClass())) {
            return new FloatParameter(this) {
                protected Object valueOf(String text) throws IOException {
                    return param.handle(text);
                }
            };
        }
        return new Parameter(this) {
            protected Object valueOf(String text) throws IOException {
                return param.handle(text);
            }
        };
    }

}
