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



import java.sql.Timestamp;
import java.text.ParsePosition;
import java.util.Calendar;

import javax.xml.namespace.QName;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.geotools.xml.impl.XsDateTimeFormat;
import org.geotools.xs.XS;

public class DateTimeBinding extends AbstractComplexBinding {
    XsDateTimeFormat format = new XsDateTimeFormat();
    GCCSDateTimeFormat gccsFormat = new GCCSDateTimeFormat();
    @Override
    public QName getTarget() {
        return XS.DATETIME;
    }

    @Override
    public Class getType() {
        // TODO Auto-generated method stub
        return Timestamp.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        ParsePosition pos;
        Calendar cal = (Calendar) format.parseObject((String)value, pos = new ParsePosition(0));

        if(cal == null) {
            cal = (Calendar) gccsFormat.parseObject((String)value, pos = new ParsePosition(0));
        }

        if(cal == null) {
            throw new IllegalArgumentException("Invalid date: \"" + value + "\" starting at: \"" +
                ((String) value).substring(pos.getErrorIndex()) + "\"");
        }

        return new Timestamp(cal.getTimeInMillis());
    }
}
