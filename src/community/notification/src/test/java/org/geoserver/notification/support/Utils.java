/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.text.SimpleDateFormat;
import org.geoserver.notification.geonode.kombu.KombuMessage;
import org.geoserver.notification.geonode.kombu.KombuSource;
import org.geoserver.notification.geonode.kombu.KombuSourceDeserializer;

public class Utils {

    public static KombuMessage toKombu(byte[] data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"));
        SimpleModule module = new SimpleModule();
        module.addDeserializer(KombuSource.class, new KombuSourceDeserializer());
        mapper.registerModule(module);
        return mapper.readValue(data, KombuMessage.class);
    }
}
