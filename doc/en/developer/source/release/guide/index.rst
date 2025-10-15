.. _release_guide:

Release Guide
=============

This guide details the process of performing a GeoServer release.

Before you start
----------------

SNAPSHOT release
^^^^^^^^^^^^^^^^

For any release (including release candidates) a GeoServer release requires the
corresponding GeoTools and GeoWebCache releases. Therefore, before you start you should
coordinate a release with these projects, either performing the release yourself or
asking a volunteer to perform the release.

* `GeoTools Release Guide <https://docs.geotools.org/latest/developer/procedures/release.html>`_
* `GeoWebCache Release repo README.md <https://github.com/GeoWebCache/gwc-release>`_

Notify developer group
^^^^^^^^^^^^^^^^^^^^^^

Post a message on the `GeoServer developer group <https://discourse.osgeo.org/c/geoserver/developer/63>`_ a few days in advance, even though the release date has been agreed upon beforehand. It is a good idea to remind developers to get any fixes 24 hours prior to release day, and to start a team discussion to identify any known blockers.

Prerequisites
-------------

The following are necessary to perform a GeoServer release:

#. Commit access to the GeoServer `Git repository <https://github.com/geoserver/geoserver>`_
#. Build access to `Jenkins <https://build.geoserver.org/jenkins/>`_
#. Edit access to the GeoServer `Blog <https://blog.geoserver.org>`_
#. Administration rights to GeoServer `Jira <https://osgeo-org.atlassian.net/projects/GEOS>`__
#. Release/file management privileges in `SourceForge <https://sourceforge.net/projects/geoserver/>`_

Versions and revisions
----------------------

When performing a release, we don't require a "code freeze" in which no developers can commit to the repository. Instead we release from a revision that is known to pass all tests, including unit/integration tests from a nightly build.

To obtain the GeoServer, GWC and GeoTools revisions that have passed testing, navigate to `geoserver.org/download > Development <https://geoserver.org/download>`__, find the correct series (e.g. 2.17.x) and download a “binary” nightly build. From the download check the :file:`src/target/VERSION.txt` file. For example:

.. code-block:: none

    version = 2.27-SNAPSHOT
    git revision = 1ee183d9af205080f1543dc94616bbe3b3e4f890
    git branch = origin/2.27.x
    build date = 19-Jul-2024 04:41
    geotools version = 27-SNAPSHOT
    geotools revision = 3bde6940610d228e01aec9de7c222823a2638664
    geowebcache version = 1.27-SNAPSHOT
    geowebcache revision = 27eec3fb31b8b4064ce8cc0894fa84d0ff97be61/27eec
    hudson build = -1

Since most GeoServer releases require official GeoTools and GeoWebCache releases, the GeoTools and GeoWebCache revisions are usually not needed - the version numbers are used instead.

Release in Jira
---------------

1. Navigate to the `GeoServer project page <https://osgeo-org.atlassian.net/projects/GEOS?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=released-unreleased>`_ in Jira.

2. Add a new version for the next version to be released after the current release. For example, if you are releasing GeoServer 2.11.5, create version 2.11.6.  Enter the current date as the Start Date and use the date from the `release schedule <https://github.com/geoserver/geoserver/wiki/Release-Schedule>`_ for the Release Date. For the final scheduled (archive) release of a series (typically 2.xx.6), do still create the next version (e.g. 2.xx.7) with a Description of `End of life, may not be released ever`.

3. Click in the Actions column for the version you are releasing and select Release. Update the Release Date to the current date when prompted. If there are still unsolved issues remaining in this release, you will be prompted to move them to an unreleased version. If so, choose the new version you created in step 2 above.

4. Check all the issues in this release for any that do not have a component and rectify (before running the ``announcement.py`` utility below.)

GeoServer release announcement
------------------------------

.. note:: Start this activity early to work on while release jobs are running. This will also serve to review resolved Jira issues before release notes are created.

The `GeoServer website <https://geoserver.org/>`_ is managed as a `GitHub Pages repository <https://github.com/geoserver/geoserver.github.io>`_.

1. Follow the `instructions <https://github.com/geoserver/geoserver.github.io#releases>`_ in the repository to create a release announcement.::

     python3 announcement.py username password 2.29.1 --geotools 34.1 --geowebcache 1.29.1
   
   For the initial release in a series you will need to make:
   
   * A ``_layout`` html template updated for any new extensions
   * A ``bin/templates/about229.md`` markdown snippet linking to new features and highlights
   
   Please read the instructions or script help for additional options beyond the example above.

2. The announcement page header fields include the information required to generate a download page for the release.
   
   The page is generated from resolved Jira tickets:

   * You may need to review closed pull requests and Jira tickets to double check what is being announced.
   
     ``https://github.com/geoserver/geoserver/compare/2.26.1...2.26.2``

   * Check for Jira tickets without a component, often these are community modules.
     
     Tip: To determine the correct component check the pull-request for the ticket. The files changed indicate
     what component has been modified.

3. Check for security vulnerability section, generated when resolved Jira issue has "vulnerability" component.
     
   **Important:** Review :ref:`security_procedure` for expectations on what to include and write here.
   
   * Security fix is initially listed with a placeholder CVE:
   
     ::
     
        ## Security Considerations
        
       This release addresses security vulnerabilities and is considered an essential upgrade for production systems.
        
        * CVE-2024-36401 Critical <!-- https://github.com/geoserver/geoserver/security/advisories/GHSA-6jj6-gm7p-fcvv -->

     
     It is your judgement call how to word this section: Every security fix is “recommended”, one with serious consequences could be considered “essential”, and an active exploit considered “urgent”.
   
   * When everyone has had an opportunity to update, the details of the vulnerability are published (editing prior blog posts).
   
     ::
   
       ## Security Considerations
       
       This release addresses security vulnerabilities and is considered an essential upgrade for production systems.
       
       * [CVE-2024-36401](https://github.com/geoserver/geoserver/security/advisories/GHSA-6jj6-gm7p-fcvv) Remote Code Execution (RCE) vulnerability in evaluating property name expression (Critical)
     
     The initial release of a series often includes several security fixes to disclose.
     
4. Review the new features, documenting each with a heading, screen snap, and thanking the appropriate developer and organization responsible. ::

      ## File System Sandbox Isolation
      
      A file system sandbox is used to limit access for GeoServer Administrators and Workspace Administrators to specified file folders.
      
      * A system sandbox is established using ``GEOSERVER_FILESYSTEM_SANDBOX`` application property, and applies to the entire application, limiting GeoServer administrators to the ``<sandbox>`` folder, and individual workspace administrators into isolated ``<sandbox>/<workspace>`` folders.
      
      * A regular sandbox can be configured from the **Security > Data** screen, and is used to limit individual workspace administrators into ``<sandbox>/<workspace>`` folders to avoid accessing each other's files.
        
        ![](/img/posts/2.26/filesystem-sandbox.png)
      
      Thanks to Andrea (GeoSolutions) for this important improvement at the bequest of [Munich RE](https://www.munichre.com/en.html).
      
      - [GSIP 229 - File system access isolation](https://github.com/geoserver/geoserver/wiki/GSIP-229)
      - [File system sandboxing](https://docs.geoserver.org/2.26.x/en/user/security/sandbox.html) (User Manual)

   
   
   
   For the initial release in a series there may be several new features to document in this manner.

5. Create a pull-request for the new website.
   
   For the initial release expect input from developers to highlight changes and work performed.

If you are cutting the first RC of a series, create the stable branch
---------------------------------------------------------------------

When creating the first release candidate of a series, there are some extra steps to create the new stable branch and update the version on the main development branch.

1. Checkout the main development branch and make sure it is up to date and that there are no changes in your local workspace::

     git checkout main
     git pull
     git status

2. Create the new stable branch and push it to GitHub; for example, if the main development branch is ``2.28-SNAPSHOT`` and the remote for the official GeoServer is called ``geoserver``::

     git checkout -b 2.28.x
     git push geoserver 2.28.x

3. Enable `GitHub branch protection <https://github.com/geoserver/geoserver/settings/branches>`_ for the new stable branch: tick "Protect this branch" (only) and press "Save changes".
   
   Check: Branch protection is configured with a wild card, but you can confirm the pattern correctly protects the branch.

3. Checkout the main development branch::

     git checkout main
    
4. Update the version in all pom.xml files; for example, if changing the main development branch from ``2.28-SNAPSHOT`` to ``2.29-SNAPSHOT``.
  
   Edit :file:`build/rename.xml` to update GeoServer, GeoTools and GeoWebCache version numbers::
   
      <property name="current" value="2.28"/>
      <property name="release" value="2.29"/>
      ..
      <replacefilter token="34-SNAPSHOT" value="35-SNAPSHOT"/>
      <replacefilter token="1.28-SNAPSHOT" value="1.29-SNAPSHOT"/>

   And then run::
     
     ant -f build/rename.xml 
    
   .. note:: use of sed
      
      To update these files using sed::
   
       find . -name pom.xml -exec sed -i 's/2.28-SNAPSHOT/2.29-SNAPSHOT/g' {} \;
 
      .. note:: ``sed`` behaves differently on Linux vs. Mac OS X. If running on OS X, the ``-i`` should be followed by ``'' -e`` for each of these ``sed`` commands.
 
      Update release artifact paths and labels, for example, if changing the main development branch from ``2.28-SNAPSHOT`` to ``2.29-SNAPSHOT``::
 
        sed -i 's/2.28-SNAPSHOT/2.29-SNAPSHOT/g' src/release/bin.xml
        sed -i 's/2.28-SNAPSHOT/2.29-SNAPSHOT/g' src/release/installer/win/GeoServerEXE.nsi
        sed -i 's/2.28-SNAPSHOT/2.29-SNAPSHOT/g' src/release/installer/win/wrapper.conf
 
      .. note:: These can be written as a single ``sed`` command with multiple files.
 
      Update GeoTools dependency; for example if changing from ``28-SNAPSHOT`` to ``29-SNAPSHOT``::
 
        sed -i 's/34-SNAPSHOT/35-SNAPSHOT/g' src/pom.xml
 
      Update GeoWebCache dependency; for example if changing from ``1.28-SNAPSHOT`` to ``1.29-SNAPSHOT``::
 
        sed -i 's/1.28-SNAPSHOT/1.29-SNAPSHOT/g' src/pom.xml
 
      Manually update hardcoded versions in configuration files:
 
      * ``doc/en/developer/source/conf.py``
      * ``doc/en/docguide/source/conf.py``
      * ``doc/en/user/source/conf.py``

5. Add the new version to the documentation index (``doc/en/index.html``) just after line 105, e.g.::

     <tr>
       <td><strong><a href="https://geoserver.org/release/2.29.x/">2.29.x</a></strong></td>
       <td><a href="2.29.x/en/user/">User Manual</a></td>
       <td><a href="2.29.x/en/developer/">Developer Manual</a></td>
     </tr>

6. Commit the changes and push to the main development branch on GitHub::

       git commit -am "Updated version to 2.29-SNAPSHOT, updated GeoTools dependency to 35-SNAPSHOT, updated GeoWebCache dependency to 1.29-SNAPSHOT, and related changes"
       git push geoserver main
      
7. Create the new RC version in `Jira <https://osgeo-org.atlassian.net/projects/GEOS>`_ for issues on the main development branch; for example, if the main development branch is now ``2.29-SNAPSHOT``, create a Jira version ``2.29.0`` for the first release of the ``2.29.x`` series

8. Update the main, nightly and live-docs jobs on build.geoserver.org:
  
   1. Disable the maintenance jobs, and remove them from the geoserver view.
    
      **Warning**: If you wish to keep the ``geoserver-<version->docs`` job for emergencies be sure to edit the live-docs build to comment out publishing to `maintain` location.::
      
        # Change this when releasing
        # LINK=maintain 
        ...
        # echo "link $VER to $LINK_PATH"
        # ssh -oStrictHostKeyChecking=no -p 2223 $REMOTE "if [ -e $LINK_PATH ]; then rm $LINK_PATH; fi && ln -s $REMOTE_PATH $LINK_PATH"
        #
        # echo "docs published to https://docs.geoserver.org/$LINK/en/user"
  
   2. Create new jobs, copying from the existing stable jobs, and edit the branch.
   3. For the previously stable version, modify the last line of `geoserver-<version>-docs`` job, changing ``stable`` to ``maintain`` so that it publishes to the ``https://docs.geoserver.org/maintain`` location.::
  
        # Change this when releasing
        LINK=maintain 

      The new job you created should publish to ``stable``, and the main development branch will continue to publish to ``latest``.
    
   4. Update the **Dashboard > Manage Jenkins > System** global properties environmental variable used by the ``geoserver-main-nightly`` docker build step to have correct name for publishing ``main`` branch.
    
      * Name: ``GEOSERVER_MAIN_DOCKER_NAME``
      * Value: ``2.29-SNAPSHOT``

9. Update the MAIN variable in Docker `release.sh <https://github.com/geoserver/docker/blob/master/build/release.sh#L6>`_ to the new main branch 2.29.

10. Announce on the developer group that the new stable branch has been created.

11. Switch to the new branch and update the documentation links, replacing ``docs.geoserver.org/latest`` with ``docs.geoserver.org/2.29.x`` (for example):
   
    * ``README.md``
    * ``doc/en/developer/source/conf.py``
    * ``doc/en/user/source/conf.py``

Build the Release
-----------------

Run the `geoserver-release` job in Jenkins:

* `geoserver-release-jdk11 <https://build.geoserver.org/view/release/job/geoserver-release-jdk11/>`__
* `geoserver-release-jdk17 <https://build.geoserver.org/view/release/job/geoserver-release-jdk17/>`__

The job takes the following parameters:

**BRANCH**

  The branch to release from, "2.29.x", "2.28.x", etc... This must be a stable branch. Releases are not performed from the main development branch.

**REV**

  The Git revision number to release from, e.g. "24ae10fe662c....". If left blank, the latest revision (i.e. HEAD) on the ``BRANCH`` being released is used.

**VERSION**

  The version/name of the release to build, "2.29.4", "2.28.2", etc...

**GT_VERSION**

  The GeoTools version to include in the release. This may be specified as a version number such as "34.0" or "33.4". Alternatively, the version may be specified as a Git branch/revision pair in the form ``<branch>@<revision>``. For example "main@36ba65jg53.....". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again, this version must be a version number corresponding to an official GeoTools release.

**GWC_VERSION**

  The GeoWebCache version to include in the release. This may be specified as a version number such as "1.29.0". Alternatively, the version may be specified as a Git revision of the form ``<branch>@<revision>`` such as "master@1b3243jb...". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT).Git Again, this version must be a version number corresponding to an official GeoTools release.

**GIT_USER**

  The Git username to use for the release.

**GIT_EMAIL**

  The Git email to use for the release.

This job will checkout the specified branch/revision and build the GeoServer
release artifacts against the GeoTools/GeoWebCache versions specified. When
successfully complete all release artifacts will be listed under artifacts in the job summary.

Additionally, when the job completes it fires off a job for a windows worker. When this job
completes it will list the ``.exe`` artifacts.

Test the Artifacts
------------------

Download and try out some of the artifacts from the above location and do a
quick smoke test that there are no issues. Engage other developers to help
test on the developer group.

It is important to test the artifacts using the minimum supported version of Java (currently Java 17 in June 2025).

Publish the Release
-------------------

Run the `geoserver-release-publish` in Jenkins:

* `geoserver-release-publish-jdk11 <https://build.geoserver.org/view/release/job/geoserver-release-publish-jdk11/>`__
* `geoserver-release-publish-jdk17 <https://build.geoserver.org/view/release/job/geoserver-release-publish-jdk17/>`__

The job takes the following parameters:

**VERSION**

  The version being released. The same value specified for ``VERSION`` when running the ``geoserver-release`` job.

**BRANCH**

  The branch being released from.  The same value specified for ``BRANCH`` when running the ``geoserver-release`` job.

This job will rsync all the artifacts located at::

     https://build.geoserver.org/geoserver/release/<RELEASE>

to the SourceForge FRS server. Navigate to `SourceForge <https://sourceforge.net/projects/geoserver/>`__ and verify that the artifacts have been uploaded properly. If this is the latest stable release, set the necessary flags (you will need to be logged in as a SourceForge admin user) on the ``.exe`` and ``.bin`` artifacts so that they show up as the appropriate default for users downloading on the Windows and Linux platforms. This does not apply to maintenance or support releases.

Cite Certification
------------------

For a major (2.xx.0) release, follow the `instructions <https://docs.geoserver.org/main/en/developer/cite-test-guide/index.html#cite-certification>`__ to obtain certification, and include it in the release notes below.

Release notes
-------------

This job will tag the release located in::
   
   https://github.com/geoserver/geoserver/tags/<RELEASE>

Publish Jira markdown release notes to GitHub tag:

#. Select the correct release from `Jira Releases <https://osgeo-org.atlassian.net/projects/GEOS?orderField=RANK&selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page&status=released>`__ page.

#. From the release page, locate the :guilabel:`Release notes` button at the top of the page to open the release notes edit
  
#. Generate release notes as markdown:
   
   * Select format `Markdown`
   * Layout: Issue key with link
   * Issue types: All
   
   Change the heading to :kbd:`Release notes`, and apply the change with :guilabel:`Done`.

   Use :guilabel:`Copy to clipboard` to obtain the markdown, similar to the following:
   
   .. code-block:: text
   
      # Release notes

      ### Bug

      [GEOS-10264](https://osgeo-org.atlassian.net/browse/GEOS-10264) Address startup warning File option not set for appender \[geoserverlogfile\]

      [GEOS-10263](https://osgeo-org.atlassian.net/browse/GEOS-10263) WPSRequestBuilderTest assumes that JTS:area is the first process in the list

      [GEOS-10255](https://osgeo-org.atlassian.net/browse/GEOS-10255) i18n user interface inconsistent layout with br tags used for layout

      [GEOS-10245](https://osgeo-org.atlassian.net/browse/GEOS-10245) jdbcconfig: prefixedName filter field not updated

      [GEOS-9950](https://osgeo-org.atlassian.net/browse/GEOS-9950) MapPreviewPage logs unable to find property: format.wfs.text/csv continuously

      ### Improvement

      [GEOS-10246](https://osgeo-org.atlassian.net/browse/GEOS-10246) jdbcconfig: performance slow-down from unnecessary transactions

      ### New Feature

      [GEOS-10223](https://osgeo-org.atlassian.net/browse/GEOS-10223) Support MBTiles in OGC Tiles API

      ### Task

      [GEOS-10247](https://osgeo-org.atlassian.net/browse/GEOS-10247) Reuse of service documentation references for workspace, metadata and default language

#. Navigate to GitHub tags https://github.com/geoserver/geoserver/tags
   
   Locate the new tag from the list, and use :menuselection:`... --> Create release`
   
   * Release title: `GeoServer 2.29.0`
   * Write: Paste the markdown from Jira release notes editor
   * Set as the latest release: only tick this for stable releases, leave unticked for maintenance and support releases
   
   Use :guilabel:`Publish release` button to publish the release notes.
   
Announce the Release
--------------------

Mailing lists
^^^^^^^^^^^^^

.. note:: This announcement should be made for all releases, including release candidates.

Post an announcement on both the Discourse User and Developer groups announcing the
release. The message should be relatively short. You can base it on the blog post headings which often indicate new features to highlight.

The following is an example::

   Subject: GeoServer 2.5.1 Released

   The GeoServer team is happy to announce the release of GeoServer 2.5.1.
  
   The release is available for download from:

   https://geoserver.org/release/2.29.0/

   GeoServer 2.29.0 is the next stable release of GeoServer and is recommended for production deployment.

   This release comes with some exciting new features. The new and
   noteworthy include:
   
   * By popular request Top/Bottom labels when configuring layer group order
   * You can now identify GeoServer “nodes” in a cluster by configuring a label and color in the UI. Documentation and example in the user guide.
   * Have you ever run GeoServer and not quite gotten your file permissions correct? GeoServer now has better logging when it cannot your data directory and is required to “fall back” to the embedded data directory during start up.
   * We have a new GRIB community module (community modules are not in the release until they pass a    QA check, but great to see new development taking shape)
   * Documentation on the jp2kak extension now in the user guide
   * Additional documentation for the image mosaic in the user guide with tutorials covering the plugin, raster time-series, time and elevation and footprint management.
   * WCS 2.0 support continues to improve with DescribeCoverage now supporting null values
   * Central Authentication Service (CAS) authentication has received a lot of QA this release and is now available in the GeoServer 2.5.x series.
   * This release is made in conjunction with GeoTools 34.0
   
   Along with many other improvements and bug fixes:
   
   * https://osgeo-org.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=10164

   Thanks to Andrea and Jody (GeoSolutions and Boundless) for publishing this release. A very special thanks to all those who contributed bug fixes, new
   features, bug reports, and testing to this release.

   --
   The GeoServer Team

OSGeo Announcement
^^^^^^^^^^^^^^^^^^

For major releases OSGeo asks that a news item be submitted:

* Login to the osgeo.org website, create a news item using the release announcement text above.

And that an announcement is sent to discuss:

* Mail major release announcements to discuss@osgeo.org (you will need to `subscribe first <https://lists.osgeo.org/listinfo/discuss>`__ ). 

