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

