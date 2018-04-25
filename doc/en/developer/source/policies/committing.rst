.. _comitting:

Committing
==========

Getting commit access
---------------------

All contributors are asked to provide an assignment agreement for working on the project:

* `corporate_contributor <https://www.osgeo.org/resources/corporate-contributor-license/>`__
* `individual_contributor <https://www.osgeo.org/resources/individual-contributor-license/>`__

This agreement can be printed, signed, scanned and emailed to `info@osgeo.org <mailto:info@osgeo.org>`_ at Open Source Geospatial Foundation (OSGeo). `OSGeo <http://www.osgeo.org/content/foundation/about.html>`_ is the  non-profit which holds the GeoServer codebase for the community.

The `contribution licenses <http://www.osgeo.org/content/foundation/legal/licenses.html>`_ are used by OSGeo projects seeking to assign copyright directly to the foundation. These licenses are directly derived from the Apache code contribution licenses (CLA V2.0 and CCLA v r190612).
   
Community commit access
^^^^^^^^^^^^^^^^^^^^^^^

The first stage of access allows a developer to commit only to the community module or extension for which they are the maintainer. This stage of access can be obtained quite easily.

The process of getting community commit access is as follows:

#. **Email the developer list**

   This first step is all about communication. In order to grant commit access
   the other developers on the project must first know what the intention is.
   Therefore any developer looking for commit access must first describe what
   they want to commit (usually a community module), and what it does.

#. **Sign up for a GitHub account**

   GeoServer source code is hosted on Github and you'll need an account in
   order to access it. You can sign-up [here <https://github.com/signup/>`_.

#. **Print, sign, scan and send the contributor agreement**
   
   * `corporate_contributor <https://www.osgeo.org/resources/corporate-contributor-license/>`_
   * `individual_contributor <https://www.osgeo.org/resources/individual-contributor-license/>`__
   
   Scanned assignment agreement can be emailed `info@osgeo.org <mailto:info@osgeo.org>`_ at OSGeo.

#. **Notify the developer list**

   After a developer has signed up on Github they must notify the developer
   list. A project despot will then add them to the group of GeoServer
   committers and grant write access to the canonical repository.

#. **Fork the canonical GeoServer repository**

   All committers maintain a fork of the GeoServer repository that they work
   from. Fork the canonical repository into your own account.

#. **Configure your local setup**

   Follow this :ref:`guide <source>` in the developer manual.

Core commit access
^^^^^^^^^^^^^^^^^^

The second allows a developer to make commits to the core modules of geoserver.
Being granted this stage of access takes time, and is obtained only after the
developer has gained the trust of the other core committers.

The process of obtaining core commit access is far less mechanical than the one
to obtain community commit access. It is based solely on trust. To obtain core
commit access a developer must first obtain the trust of the other core
committers.

The way this is typically done is through continuous code review of patches.
After a developer has submitted enough patches that have been met with a
positive response, and those patches require little modifications, the
developer will be nominated for core commit access.

There is no magic number of patches that make the criteria, it is based mostly
on the nature of the patches, how in depth the they are, etc... Basically it
boils down to the developer being able to show that they understand the code base
well enough to not seriously break anything.

Commit Guidelines
-----------------

There is not much in the way of strict commit policies when it comes to committing
in GeoServer. But over time some rules and conventions have emerged:

#. **Update copyright headers**

   When adding new source files to the repository remember to add the standard
   copyright header::

      /* (c) 2016 Open Source Geospatial Foundation - all rights reserved
       * This code is licensed under the GPL 2.0 license, available at the root
       * application directory.
       */

   When updating a file that does not have an existing header, add one (as above).

   When adding content from another organisation maintain copyright history and original license. Only add Open Source Geospatial Foundation if you have made modifications to the file for GeoServer::
   
      /* (c) 2016 Open Source Geospatial Foundation - all rights reserved
       * (c) 2014 OpenPlans
       * (c) 2008-2010 GeoSolutions
       * 
       * This code is licensed under the GPL 2.0 license, available at the root
       * application directory.
       * 
       * Original from GeoWebCache 1.5.1 under a LGPL license 
       */

   In a rare case (as when asking to migrate content from GeoTools) you can obtain permission to change the license to our GPL 2.0 license.

#. **Do not commit large amounts of binary data**

   In general do not commit any binary data to the repository. There are cases where
   it is appropriate like some data for a test case, but in these cases the files
   should be kept as small as possible.

#. **Do not commit jars or libs, use Maven instead**

   In general never commit a depending library directly into the repository, this is
   what we use Maven for. If you have a jar that is not present in any maven
   repositories, ask on the developer list to get it uploaded to one of the project
   maven repositories.

#. **Ensure code is properly formatted**

   Ensure that the IDE or editor used to edit source files is setup with proper
   formatting rules. This means spaces instead of tabs, 100 character line break,
   etc...

   If using Eclipse ensure you have configured it with the `template and formatter <http://docs.geotools.org/latest/developer/conventions/code/style.html#use-of-formatting-tools>`_
   used for GeoTools.


