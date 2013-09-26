/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client.xml;



import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class IgnoredTypeBinding extends AbstractComplexBinding {
    public final QName target;

    public IgnoredTypeBinding(QName target) {
        this.target = target;
    }

    @Override
    public QName getTarget() {
        return target;
    }

    @Override
    public Class getType() {
        // For some reason, the parser is expecting a Collection for unknown types
        return Collection.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        return Collections.EMPTY_LIST;
    }
}
