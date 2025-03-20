.. _community_modules:

Community Modules
=================

This document describes the GeoServer community module process. It is a guide 
that describes how the GeoServer project takes in contributions from the 
community.

In GeoServer a module can fall into one of three classes:

* **core**, those modules which GeoServer requires to function and are 
  distributed with the main GeoServer distribution
* **extension**, plug-ins available as separate artifacts from the main 
  distribution
* **community**, experimental or unstable modules which are not part of the 
  release process

Every module added to GeoServer has its origin as a community module. If the 
module becomes stable enough it will eventually become part of the main 
GeoServer distribution either as a core module, or as an extension.

Creating a community module
---------------------------

Requirements
^^^^^^^^^^^^

The single requirement for adding a community module is the approval of one 
Project Steering Committee member. 

Process
^^^^^^^

The following outlines the steps to be taken in order to add a new community module.

#. **Get approval**

   The first step is to get approval to add the community module. This 
   involves first explaining to the rest of the GeoServer community the 
   purpose and function of the extension to be added. The two best ways to
   do this are:

   #.  send an email to the developers list, or
   #.  participate in a weekly IRC meeting

   After explaining the intention, the approval of at least one Project 
   Steering Committee member is needed before proceeding. Getting approval is
   easy as long as it is explained that the extension will be useful to other 
   users or developers.

#. **Get version control access**

   The next step is to create the community module in the git 
   repository. To do this it is necessary to be granted commit status. The 
   process for signing up for version control access is defined in the 
   :ref:`comitting` section.

#. **Add a new module**

   Once commit access is obtained the module can be added. All community 
   modules live under the directory ``community``, directly under the root of
   the source tree. The community modules on trunk can be found 
   `here <https://github.com/geoserver/geoserver/tree/main/src/community>`_.

   For example, from the root of the GeoServer source tree::

     [geoserver]% cd src/community
     [geoserver/src/community]% mkdir myCommunityModule
     [geoserver/src/community]% git add myCommunityModule
     [geoserver/src/community]% git commit -m "adding my community module"

#. **Add a Maven POM** 
  
   Every module in the build requires a maven pom file, ``pom.xml``. Use the 
   following as a template:

     .. code-block:: xml

          <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
          <modelVersion>4.0.0</modelVersion>

          <parent>
            <groupId>org.geoserver</groupId>
            <artifactId>geoserver</artifactId>
            <version>2.8-SNAPSHOT</version> <!-- change this to the proper GeoServer version -->
          </parent>

          <groupId>org.geoserver</groupId>
          <artifactId>myCommunityModule</artifactId>
          <version>1.0-SNAPSHOT</version>
          <packaging>jar</packaging>
          <name>My Community Module</name>

          <dependencies>
            <!-- add any dependencies your module has here -->
          </dependencies>
        </project>
     
   Add the file to the root of the new community module, 
   ``myCommunityModule/pom.xml``

#. **Add a build profile**

   The final step involves adding the new module to the maven build, and in 
   particular adding a build profile for it. To do this:

   #. Edit ``community/pom.xml`` and add the following inside of the 
      ``<profiles>`` element:

      .. code-block:: xml

           <profiles>
             ...
             <profile>
               <id>myCommunityModule</id>
               <modules>
                 <module>myCommunityModule</module>
               </modules>
             </profile>
           </profiles>

   #. Edit ``web/app/pom.xml`` and add the following inside of the ``<profiles>``
      element:

      .. code-block:: xml

           <profiles>
             ...
             <profile>
               <id>myCommunityModule</id>
               <dependencies>
                 <dependency>
                    <groupId>org.geoserver</groupId>
                    <artifactId>myCommunityModule</artifactId>
                    <version>1.0-SNAPSHOT</version>
                  </dependency>
               </dependencies>
             </profile>
           </profiles>

   .. warning::

      If the community module depends on any other community modules,
      they too should be included in the profile definition.

   .. warning::

      Ensure that the name of the profile matches the name of the
      community module

Promoting a community module
----------------------------

Once a community modules becomes "stable", it may be promoted to a core or 
extension module. Which depends on the nature of the community module. If the 
module is plug-in based (i.e. it provides functionality that some users may want,
but others may not) then it should become an extension. Otherwise it should 
become a core module.

Requirements
^^^^^^^^^^^^

The following properties must hold true in order to promote a community module:

#. **The module is not site-specific and can be configured for use by the general GeoServer community.**

   A community module of interest to multiple users would meet this goal; while a community module that has hard-coded a domain name would not.

#. **The module has a designated and active maintainer**

   Every core and extension module requires a module maintainer. The job of 
   the maintainer is to fix bugs and address issues which arise with the 
   module. If a community module is promoted and the maintainer "drops off", 
   the module is in danger of being demoted back to community status.

#. **The module is considered "stable" by the majority of the PSC**

   A module will only be promoted if it is deemed "stable" by the majority of
   the PSC. Those PSC members deeming it "unstable" must provide a reasonable
   justification for the assertion.

#. **The module maintains 40% test coverage**

   A minimum of 40% test coverage must be maintained by the module in order to
   be promoted. Of course higher coverage is encouraged. The more test 
   coverage a community module the more credibility it gets.

#. **The module has no IP violations**

   The module must not contain any code with a license or copyright that 
   violates the GPL.

#. **The module has a page in the user manual**

   Each module needs a page in the user manual documenting its function and 
   usage. Tutorials and walk-throughs are encouraged.

#. **The maintainer has signed the GeoServer Contributor Agreement**

   OSGeo retains all copyright on code released as
   part of GeoServer. Since core and extension modules are released along with
   the rest of GeoServer, the maintainer of said modules must agree to assign
   copyright of code to OSGeo.

Process
^^^^^^^

#. **Submit a GeoServer Improvement Proposal**

   To promote a community module the contributor must create a 
   :ref:`gsip` (GSIP). The proposal must 
   then go through the regular feedback and voting process.

#. **Move the module**

   Once the proposal is accepted, the next step is to move the module out of 
   the community space. Where the module ends up depends on whether it is being
   promoted to a core module, or an extension.

   *Core modules*

   Core modules live under the root of the source tree::

     [geoserver]% mv src/community/myCommunityModule src/
     [geoserver]% git add src/myCommunityModule
     [geoserver]% git add --all src/community/myCommunityModule
     [geoserver]% git commit -m "promoting my community module to a core module"

   *Extensions*

   Extension modules live under the extension directory, under the root of the
   source tree::

     [geoserver]% mv src/community/myCommunityModule src/extension
     [geoserver]% git add src/extension/myCommunityModule
     [geoserver]% git add --all src/community/myCommunityModule
     [geoserver]% git commit -m "promoting my community module to an extension"

#. **Update the build**

   Once the module has been moved, the maven build must be updated. 

   *Core modules*

   #. Edit ``community/pom.xml`` and remove the profile for the community 
      module
   #. Edit ``pom.xml`` under the root of the source tree and add a module 
      entry::

            <modules>
              ...
              <module>myCommunityModule</module>
            </modules>

   #. Edit ``web/app/pom.xml`` and move the dependency on the community module 
        into the main dependencies section of the pom. Then remove the profile

   *Extensions*

   #. Copy the profile for the community module from ``community/pom.xml`` 
      to ``extension/pom.xml``
   #. Remove the profile from ``community/pom.xml``
   #. Remove the release descriptor from ``community/pom.xml`` contained in the maven-assembly-plugin configuration section
   #. Remove the dependency from ``community/release/pom.xml``

#. **Update the release process**

   The next step is to include the new module in the release process.

   *Extensions*

   #. Create a new directory under ``release/extensions`` which matches the
      name of the extension
   #. Add the following to the new directory:
  
      #. A license called :file:`<module>-LICENSE.md` which contains the license notice
         for the extension (linking to full `licenses/` documents included below).
         
         Follow the :download:`h2-LICENSE.md </../../../../src/release/extensions/h2/h2-LICENSE.md>` example:
         
         .. literalinclude:: /../../../../src/release/extensions/h2/h2-LICENSE.md
            :language: markdown

      #. A readme called :file:`<module>-README.md` which contains instructions 
         on how to install the extension.
         
         Follow the :download:`h2-README.md </../../../../src/release/extensions/h2/h2-README.md>` example:
         
         .. literalinclude:: /../../../../src/release/extensions/h2/h2-README.md
            :language: markdown
            
         .. warning::

            Don't skip this step.

      #. Any "static" files that are required by the extension.
         
         An example would be data files or a proprietary driver not available for download via maven.

   #. Create a release assembly called :file:`ext-<module>.xml` under the release directory.
      
      Follow the example of :download:`ext-h2-xml </../../../../src/release/ext-h2.xml>`:
      
      .. literalinclude:: /../../../../src/release/ext-h2.xml
         :language: xml
         
      * Add additional ``include`` elements in the root folder (outputDirectory empty) for
        the jar dependencies of the module 
      * Add additional ``include`` elements in the licenses folder (outputDirectory ``licenses``) for
        licenses required
      * Add an additional fileSet if there are any static file dependencies of the module required by the module
      * Use ``file`` with ``desName`` for any individual files that require renaming

   #. Add a dependency from ``release/pom.xml`` to the extension 
      module:
      
      .. code-block:: xml

         <dependencies>
            ...
            <dependency>
              <groupId>org.geoserver.extension</groupId>
              <artifactId>%module%</artifactId>
              <version>%version%</version>
            </dependency>
            ...
          </dependencies>

   #. Add an entry for the release descriptor to the root ``pom.xml`` of
      the source tree (i.e. one step up from the release directory):
      
      .. code-block:: xml

         <!-- artifact assembly -->
         <plugin>
           <artifactId>maven-assembly-plugin</artifactId>
           <version>2.1</version>
           <configuration>
             <descriptors>
              <descriptor>release/war.xml</descriptor>
              <descriptor>release/javadoc.xml</descriptor>
              <descriptor>release/bin.xml</descriptor>
              <descriptor>release/doc.xml</descriptor>
              ...
              <descriptor>release/ext-%module%.xml</descriptor>
             </descriptors>
           </configuration>
         </plugin>

    #. Update the documentation

       Add a page to the user manual for the new module. 

       .. todo:: 
 
          Finish this by linking somewhere...

    #. Download and a contributor license agreement as pdf for txt file:

       * `Individual Contributor License Agreement <https://www.osgeo.org/resources/individual-contributor-license/>`_
       
       * `Software Grant and Corporate Contributor License Agreement <https://www.osgeo.org/resources/corporate-contributor-license/>`_
         
         This option can also be used as a "software grant" to donate a specific named contribution in its entirety,
         as was done for GeoFence, and indeed GeoServer itself.
       
    #. Follow the instructions on the form to submit it.
     
Demoting a community module
---------------------------

For one reason or another a module is neglected and becomes unmaintained. When 
this happens the GeoServer PSC essentially becomes the maintainer and may decide
to do one of two things:

#. **Assume maintainership**

   In this case someone (may be more than one person) on the PSC agrees to 
   take on maintainership duties responsibilities for the module, such as bug
   fixing
  
#. **Demote the module**

   If no one steps up to maintain the module it **may** be demoted back to 
   community status. If and when a module is demoted depends on the 
   circumstances. If the module is relatively "quiet" in that it just works 
   and not many bug reports arise from it, it may be left alone and not 
   demoted.

Requirements
^^^^^^^^^^^^

The following properties must hold true in order to demote a module back to 
community status:
 
#. **The module has no designated maintainer**

   The module maintainer has stepped down or is unreachable and has not been 
   active for a number of weeks.

#. **The module is problematic**

   The module contains one or more issues with blocker status, or contains a 
   "handful" of issues with high priority.

Process
^^^^^^^

The following outlines the steps to demote a module to community status: 

#. **Call for a maintainer**

   Before demoting the module first try to find a new maintainer for it. Both notify
   GeoServer Devel mailing list and GeoServer User forum that module is in 
   danger of reverting to community status. Wait a few days to see 
   if anyone steps up to take on maintainership or provide funding.

#. **Move the module and update the build**

   If no one steps up to take on the maintainer role, reverse the steps 
   described here, taken to promote the module. In summary:

   #. Move the module back to the ``community`` directory
   #. Disable any of the modules release artifacts
   #. Move the profile for the module from ``extension/pom.xml`` to 
      ``community/pom.xml`` in the case of an extension module

Stepping down from module maintainership
----------------------------------------

Often a module maintainer does not have the time or resources to continue to
maintain a contribution. This is understood and is a fact of life in the open
source software world. However, to relieve the burden on the project and PSC, 
the following steps taken by any maintainer stepping down are highly 
appreciated.

#. **Give notice**

   The more time you can give to the project in lieu of your departure the 
   better. Send an email to the developers list as soon as you know you will 
   be dropping off.

#. **Find a new maintainer**

   While often not possible, any attempt to find a new maintainer for the 
   module is greatly appreciated - maybe someone who has contributed to the module before.
