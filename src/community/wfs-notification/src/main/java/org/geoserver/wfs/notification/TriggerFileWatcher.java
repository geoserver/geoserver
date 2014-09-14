/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

class TriggerFileWatcher extends AbstractFileWatcher<Map<QName, List<Trigger>>> {
    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(Triggers.class);
        } catch(JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public TriggerFileWatcher(long checkPeriod, URL u) {
        super(checkPeriod, Collections.EMPTY_MAP, u);
    }

    @Override
    protected Map<QName, List<Trigger>> doLoad(URLConnection conn) {
        Triggers triggers = null;
        InputStream inputStream = null;
        try {
            Unmarshaller unm = CTX.createUnmarshaller();
            try {
                unm.setProperty("com.sun.xml.bind.ObjectFactory",
                    new ObjectFactoryEx());
            } catch (PropertyException pe) {
                try {
                    unm.setProperty("com.sun.xml.internal.bind.ObjectFactory",
                        new ObjectFactoryEx());
                } catch(PropertyException pe2) {

                }
            }
            inputStream = conn.getInputStream();
            triggers = unm.unmarshal(new StreamSource(inputStream), Triggers.class).getValue();
        } catch(JAXBException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        if(triggers == null)
            return Collections.EMPTY_MAP;

        final HashMap<QName, List<Trigger>> map =
            new HashMap<QName, List<Trigger>>(triggers.getFeature().size());
        for(Feature f : triggers.getFeature())
            map.put(f.getType(), f.getTrigger());
        return Collections.unmodifiableMap(map);
    }
}
