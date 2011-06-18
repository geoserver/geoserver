.. _comitting:

Comitting
=========

Getting commit access
---------------------

There are two stages of commit access:

#. community module or extension commit access
#. core commit access

The first stage of access allows a developer to commit only to the community
module or extension for which they are the maintainer. This stage of access can
be obtained quite easily. 

The second allows a developer to make commits to the core modules of geoserver.
Being granted this stage of access takes time, and is obtained only after the 
developer has gained the trust of the other core comitters.

Community commit access
^^^^^^^^^^^^^^^^^^^^^^^

The process of getting community commit access is as follows:

#. **Email the developer list**  

   This first step is all about communication. In order to grant commit access
   the other developers on the project must first know what the intention is.
   Therefore any developer looking for commit access must first describe what
   they want to commit (usually a community module), and what it does.

#. **Join the project on CodeHaus**

   After a developer has stated intentions via the email list, a PSC member 
   will approve the request for commit access. At which point the developer 
   must:

   #. Create a Codehaus account by visiting http://xircles.codehaus.org/signup

      .. note::

         If you already have a JIRA (bug tracker) or Confluence (wiki) 
         account it is best to use the same userid for the Codehaus account.

   #. Visit http://xircles.codehaus.org/projects/geoserver
   #. Click the ``Apply to join as a developer`` link

      .. image:: codehaus-join.jpg
         :width: 450

#. Notify the developer list

   After a developer has signed up on Codehaus they must notify the developer
   list. A project despot will then approve the request to join the project.

Core commit access
^^^^^^^^^^^^^^^^^^

The process of obtaining core commit access is far less mechanical than the one
to obtain community commit access. It is based soley on trust. To obtain core
commit access a developer must first obtain the trust of the other core 
commiters.

The way this is typically done is through continuous code review of patches. 
After a developer has submitted enough patches that have been met with a 
postitive response, and those patches require little modifications, the 
developer will be granted core commit access. 

There is no magic number of patches that make the criteria, it is based mostly
on the nature of the patches, how in depth the they are, etc... Basically it 
boils down the developer being able to show that they understand the code base
well enough to not seriously break anything.

Commit Guidelines
-----------------

There is not much in the way of strict commit policies when it comes to committing
in GeoServer. But over time some rules and conventions have emerged:

#. **Add copyright headers for new files**

   When adding new source files to the repository remember to add the standard 
   copyright header::

     /* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
      * This code is licensed under the GPL 2.0 license, available at the root
      * application directory.
      */

#. **Do not commit large amounts of binary data**

   In general do not commit any binary data to the repository. There are cases where
   it is appropriate like some data for a test case, but in these cases the files 
   should be kept as small as possible.

#. **Do not commit jars or libs, use Maven instead**

   In general never commit a depending library directly into the repository, this is
   what we use Maven for. If you have a jar that is not present in any maven 
   repositories ask on the developer list to get it uploaded to one of the project
   maven repositories.

#. **Ensure code is properly formatted**

   Ensure that the IDE or editor used to edit source files is setup with proper 
   formatting rules. This means spaces instead of tabs, 100 character line break,
   etc...

   If using Eclipse ensure you have configured it with the `template and formatter <http://docs.geotools.org/latest/developer/guide/conventions/code/style.html#use-of-formatting-tools>`_
   used for GeoTools. 


