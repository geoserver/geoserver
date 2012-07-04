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

#. **Sign up for a GitHub account**

   GeoServer source code is hosted on Github and you'll need an account in 
   order to access it. You can sign-up `here <https://github.com/signup/>`_.
   
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

