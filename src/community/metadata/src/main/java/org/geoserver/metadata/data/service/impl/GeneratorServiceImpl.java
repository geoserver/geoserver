/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.metadata.data.service.ComplexAttributeGenerator;
import org.geoserver.metadata.data.service.GeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeneratorServiceImpl implements GeneratorService {

    private Map<String, ComplexAttributeGenerator> map =
            new HashMap<String, ComplexAttributeGenerator>();

    public void register(ComplexAttributeGenerator generator) {
        map.put(generator.getType(), generator);
    }

    @Override
    public ComplexAttributeGenerator findGeneratorByType(String typeName) {
        return map.get(typeName);
    }

    @Autowired(required = false)
    public void setGenerators(List<ComplexAttributeGenerator> generators) {
        for (ComplexAttributeGenerator generator : generators) {
            register(generator);
        }
    }
}
