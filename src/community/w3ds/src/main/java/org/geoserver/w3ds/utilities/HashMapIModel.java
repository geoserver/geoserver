/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.model.IModel;


@SuppressWarnings("serial")
public abstract class HashMapIModel implements IModel {
    IModel wrapped;
    String objectName;

    public HashMapIModel(IModel wrapped, String objectName) {
        if (wrapped == null)
            throw new NullPointerException(
                    "Live list model cannot wrap a null model");
        this.wrapped = wrapped;
        this.objectName = objectName;
    }

    public void setObject(Object object) {
    	HashMap hashMap = (HashMap) wrapped.getObject();
    	List list = null;
    	if(object.getClass().isAssignableFrom(String.class)) {
			list = W3DSUtils.parseStrArray((String)object);
		}
		else {
			list = (List) object;
		}
    	if(hashMap.containsKey(this.objectName)) {
    		object = hashMap.get(this.objectName);
    		List list_aux = null;
    		if(object.getClass().isAssignableFrom(String.class)) {
    			list_aux = W3DSUtils.parseStrArray((String)object);
    		}
    		else {
    			list_aux = (List) object;
    		}
    		for(String cn : (List<String>)list) {
    			if(!list_aux.contains(cn)) {
    				list_aux.add(cn);
    			}
    		}
    		hashMap.remove(this.objectName);
    		hashMap.put(this.objectName, list_aux);
    	}
    	else {
    		hashMap.put(this.objectName, list);
    	}
    }

    public void detach() {
        wrapped.detach();
    }
    
    public static HashMapIModel hashMap(IModel wrapped, String objectName) {
        return new HashMapIModel(wrapped, objectName) {

            public Object getObject() {
            	HashMap aux = (HashMap) wrapped.getObject();
                ArrayList list = null;
            	if(aux.containsKey(this.objectName)) {
            		Object o = aux.get(this.objectName);
            		if(o.getClass().isAssignableFrom(String.class)) {
        				return W3DSUtils.parseStrArray((String)o);
        			}
        			else {
        				return (List) o;
        			}
            	}
            	return new ArrayList();
            }
            
        };
    }
}
