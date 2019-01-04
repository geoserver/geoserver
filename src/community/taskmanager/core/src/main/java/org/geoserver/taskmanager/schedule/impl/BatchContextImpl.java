/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class BatchContextImpl implements BatchContext {

    private static class TempObject {
        public Object tempValue;
        public List<Dependency> dependencies = new ArrayList<Dependency>();
    }

    private BatchRun batchRun;

    private Map<Object, TempObject> tempValues = new HashMap<Object, TempObject>();

    public BatchContextImpl(BatchRun batchRun) {
        this.batchRun = batchRun;
    }

    @Override
    public Object get(Object original) {
        return get(original, null);
    }

    @Override
    public Object get(Object original, Dependency dependency) {
        TempObject to = tempValues.get(original);
        if (to != null) {
            if (dependency != null) {
                to.dependencies.add(dependency);
            }
            return to.tempValue;
        } else {
            return original;
        }
    }

    @Override
    public void put(Object original, Object temp) {
        TempObject to = new TempObject();
        to.tempValue = temp;
        tempValues.put(original, to);
    }

    @Override
    public void delete(Object original) throws TaskException {
        TempObject to = tempValues.remove(original);
        if (to != null) {
            for (Dependency dep : to.dependencies) {
                dep.revert();
            }
        }
    }

    @Override
    public BatchRun getBatchRun() {
        return batchRun;
    }
}
