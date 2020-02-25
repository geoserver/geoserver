/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import java.util.List;
import org.geoserver.taskmanager.util.LookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Look-up service for actions.
 *
 * @author Niels Charlier
 */
@Service
public class LookupActionServiceImpl extends LookupServiceImpl<Action> {

    @Autowired(required = false)
    public void setActions(List<Action> actions) {
        setNamed(actions);
    }
}
