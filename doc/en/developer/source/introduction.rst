.. _introduction:

Introduction
============

Welcome to GeoServer development. The project makes use of a number of resources:

* https://github.com/geoserver/geoserver.github.io/wiki Wiki used for Proposals
* https://github.com/geoserver/geoserver Github source code
* https://osgeo-org.atlassian.net/projects/GEOS Jira issue tracker
* `GeoServer User Manual <http://docs.geoserver.org/latest/en/user/>`_
* `GeoServer Developer Manual <http://docs.geoserver.org/latest/en/developer/>`_

Communication channels:

* http://blog.geoserver.org/
* `geoserver-devel <http://lists.sourceforge.net/mailman/listinfo/geoserver-devel>`_ email list
* `geoserver-users <http://lists.sourceforge.net/mailman/listinfo/geoserver-users>`_ email list
* https://gitter.im/geoserver/geoserver

We have a number of build servers employed to assist with day to day activities:

* https://build.geoserver.org/view/geoserver/ (main build server)
* http://office.geo-solutions.it/jenkins/ (windows build server)

Notification email lists:

* https://groups.google.com/forum/#!forum/geoserver-commits
* https://groups.google.com/forum/#!forum/geoserver-extra-builds

Question and answer:

* http://gis.stackexchange.com/questions/tagged/geoserver
* http://stackoverflow.com/questions/tagged/geoserver

License
-------

For complete license information review :download:`LICENSE.txt </../../../../LICENSE.txt>`.

* GeoServer is free software and is licensed under the :download:`GNU General Public License </../../../../src/release/GPL.txt>`::
  
    GeoServer, open geospatial information server
    Copyright (C) 2014 - Open Source Geospatial Foundation
    Copyright (C) 2001 - 2014 OpenPlans

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version (collectively, "GPL").

    As an exception to the terms of the GPL, you may copy, modify,
    propagate, and distribute a work formed by combining GeoServer with the
    Eclipse Libraries, or a work derivative of such a combination, even if
    such copying, modification, propagation, or distribution would otherwise
    violate the terms of the GPL. Nothing in this exception exempts you from
    complying with the GPL in all respects for all of the code used other
    than the Eclipse Libraries. You may include this exception and its grant
    of permissions when you distribute GeoServer.  Inclusion of this notice
    with such a distribution constitutes a grant of such permissions.  If
    you do not wish to grant these permissions, remove this paragraph from
    your distribution. "GeoServer" means the GeoServer software licensed
    under version 2 or any later version of the GPL, or a work based on such
    software and licensed under the GPL. "Eclipse Libraries" means Eclipse
    Modeling Framework Project and XML Schema Definition software
    distributed by the Eclipse Foundation and licensed under the Eclipse
    Public License Version 1.0 ("EPL"), or a work based on such software and
    licensed under the EPL.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA

Additionally:

* Several files supporting GZIP compressions (GZIPFilter, GZIPResponseStream, GZIPResponseWrapper
  are provided using::

    /*
     * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
     * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
     * http://www.jspbook.com. You may freely use the code both commercially
     * and non-commercially. If you like the code, please pick up a copy of
     * the book and help support the authors, development of more free code,
     * and the JSP/Servlet/J2EE community.
     *
     * Modified by David Winslow <dwinslow@openplans.org>
     */

* SetCharacterEncodingFilter and RewindableInputStream makes use of code provided
  under :download:`Apache License Version 2.0 </../../../../src/release/apache-2.0.txt>`.

* UCSReader is provided using :download:`Apache License Version 1.1 </../../../../src/release/apache-1.1.txt>`.

* Snippets from the Prototype library (www.prototypejs.org) under a MIT license.

* The build process will download jars from JAI ImageIO (BSD), Jetty (Jetty License), EMF (EPL), XSD (EPL). Several projects using the Apache License 2.0: Spring, Apache Commons, Log4j, Batik, Xerces.