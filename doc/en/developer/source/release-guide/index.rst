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

* `GeoTools Release Guide <http://docs.geotools.org/latest/developer/procedures/release.html>`_
* `GeoWebCache Release repo README.md <https://github.com/GeoWebCache/gwc-release>`_

Notify developer list
^^^^^^^^^^^^^^^^^^^^^

Send an email to the `GeoServer developer list <https://lists.sourceforge.net/lists/listinfo/geoserver-devel>`_ a few days in advance, even though the release date has been agreed upon before hand. It is a good idea to remind developers to get any fixes 24 hours prior to release day, and to start a team discussion to identify any known blockers.

Prerequisites
-------------

The following are necessary to perform a GeoServer release:

#. Commit access to the GeoServer `Git repository <https://Github.com/geoserver/geoserver>`_
#. Build access to `Jenkins <http://build.geoserver.org/jenkins/>`_
#. Edit access to the GeoServer `Blog <http://blog.geoserver.org>`_
#. Administration rights to GeoServer `JIRA <https://osgeo-org.atlassian.net/projects/GEOS>`__
#. Release/file management privileges in `SourceForge <https://sourceforge.net/projects/geoserver/>`_

Versions and revisions
----------------------

When performing a release, we don't require a "code freeze" in which no developers can commit to the repository. Instead, we release from a revision that is known to pass all tests, including unit/integration tests as well as CITE tests.

To obtain the GeoServer and GeoTools revisions that have passed the `CITE test <https://build.geoserver.org/view/testing-cite/>`_, navigate to the latest Jenkins run of the CITE test  and view its console output and select to view its full log. For example:

    https://build.geoserver.org/job/2.11-cite-wms-1.1/286/consoleText

Perform a search on the log for 'git revision' (this is the GeoServer revision) and you should obtain the following:

.. code-block:: none

    version = 2.11-SNAPSHOT
    git revision = 08f43fa77fdcd0698640d823065b6dfda7f87497
    git branch = origin/2.11.x
    build date = 18-Dec-2017 19:51
    geotools version = 17-SNAPSHOT
    geotools revision = a91a88002c7b2958140321fbba4d5ed0fa85b78d
    geowebcache version = 1.11-SNAPSHOT
    geowebcache revision = 0f1cbe9466e424621fae9fefdab4ac5a7e26bd8b/0f1cb

Since most GeoServer releases require an official GeoTools release, the GeoTools revision is usually not needed.

Release in JIRA
---------------

1. Navigate to the `GeoServer project page <https://osgeo-org.atlassian.net/projects/GEOS?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=released-unreleased>`_ in JIRA.

2. Add a new version for the next version to be released after the current release. For example, if you are releasing GeoServer 2.11.5, create version 2.11.6.

3. Click in the Actions column for the version you are releasing and select Release. Enter the release date when prompted. If there are still unsolved issues remaining in this release, you will be prompted to move them to an unreleased version. If so, choose the new version you created in step 2.

If you are cutting the first RC of a series, create the stable branch
---------------------------------------------------------------------

When creating the first release candidate of a series, there are some extra steps to create the new stable branch and update the version on the main development branch.

* Checkout the main development branch and make sure it is up to date and that there are no changes in your local workspace::

    git checkout main
    git pull
    git status

* Create the new stable branch and push it to GitHub; for example, if the main development branch is ``2.11-SNAPSHOT`` and the remote for the official GeoServer is called ``geoserver``::

    git checkout -b 2.11.x
    git push geoserver 2.11.x

* Enable `GitHub branch protection <https://github.com/geoserver/geoserver/settings/branches>`_ for the new stable branch: tick "Protect this branch" (only) and press "Save changes".

* Checkout the main development branch::

    git checkout main
    
* Update the version in all pom.xml files; for example, if changing the main development branch from ``2.17-SNAPSHOT`` to ``2.18-SNAPSHOT``.
  
  Edit :file:`build/rename.xml` to update GeoServer, GeoTools and GeoWebCache version numbers::
  
     <property name="current" value="2.17"/>
     <property name="release" value="2.18"/>
     ..
     <replacefilter token="23-SNAPSHOT" value="24-SNAPSHOT"/>
     <replacefilter token="1.17-SNAPSHOT" value="1.18-SNAPSHOT"/>

     
  And then run::
    
    ant -f build/rename.xml 
    
  .. note:: use of sed
     
     To update these files using sed::
  
      find . -name pom.xml -exec sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' {} \;

     .. note:: ``sed`` behaves differently on Linux vs. Mac OS X. If running on OS X, the ``-i`` should be followed by ``'' -e`` for each of these ``sed`` commands.

     Update release artifact paths and labels, for example, if changing the main development branch from ``2.11-SNAPSHOT`` to ``2.12-SNAPSHOT``::

       sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/bin.xml
       sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/installer/win/GeoServerEXE.nsi
       sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/installer/win/wrapper.conf

     .. note:: These can be written as a single ``sed`` command with multiple files.

     Update GeoTools dependency; for example if changing from ``17-SNAPSHOT`` to ``18-SNAPSHOT``::

       sed -i 's/17-SNAPSHOT/18-SNAPSHOT/g' src/pom.xml

     Update GeoWebCache dependency; for example if changing from ``1.11-SNAPSHOT`` to ``1.12-SNAPSHOT``::

       sed -i 's/1.11-SNAPSHOT/1.12-SNAPSHOT/g' src/pom.xml

     Manually update hardcoded versions in configuration files:

     * ``doc/en/developer/source/conf.py``
     * ``doc/en/docguide/source/conf.py``
     * ``doc/en/user/source/conf.py``

* Add the new version to the documentation index (``doc/en/index.html``) just after line 105, e.g.::

    <tr>
      <td><strong><a href="http://geoserver.org/release/2.12.x/">2.12.x</a></strong></td>
      <td><a href="2.12.x/en/user/">User Manual</a></td>
      <td><a href="2.12.x/en/developer/">Developer Manual</a></td>
    </tr>

* Commit the changes and push to the main development branch on GitHub::

      git commit -am "Updated version to 2.12-SNAPSHOT, updated GeoTools dependency to 18-SNAPSHOT, updated GeoWebCache dependency to 1.12-SNAPSHOT, and related changes"
      git push geoserver main
      
* Create the new RC version in `JIRA <https://osgeo-org.atlassian.net/projects/GEOS>`_ for issues on the main development branch; for example, if the main development branch is now ``2.12-SNAPSHOT``, create a Jira version ``2.12-RC1`` for the first release of the ``2.12.x`` series

* Update the main, nightly and live-docs jobs on build.geoserver.org:
  
  * disable the maintenance jobs, and remove them from the geoserver view
  * create new jobs, copying from the existing stable jobs, and edit the branch.
  * modify the last line of the live-docs builds, changing ``stable`` to ``maintain`` for the previous stable branch. The new job you created should publish to ``stable``, and the main development branch will continue to publish to ``latest``.

* Update the cite tests on build.geoserver.org:

  * disable the maintenance jobs, and remove them from the geoserver view
  * create new jobs, copying from the existing main development branch jobs, editing the branch in the build command.

* Announce on the developer mailing list that the new stable branch has been created.

* Switch to the new branch and update the documentation links, replacing ``docs.geoserver.org/latest`` with ``docs.geoserver.org/2.12.x`` (for example):
   
  * ``README.md``
  * ``doc/en/developer/source/conf.py``
  * ``doc/en/user/source/conf.py``

Build the Release
-----------------

Run the `geoserver-release <https://build.geoserver.org/view/geoserver/job/geoserver-release/>`_ job in Jenkins. The job takes the following parameters:

**BRANCH**

  The branch to release from, "2.2.x", "2.1.x", etc... This must be a stable branch. Releases are not performed from the main development branch.

**REV**

  The Git revision number to release from, e.g. "24ae10fe662c....". If left blank, the latest revision (i.e. HEAD) on the ``BRANCH`` being released is used.

**VERSION**

  The version/name of the release to build, "2.1.4", "2.2", etc...

**GT_VERSION**

  The GeoTools version to include in the release. This may be specified as a version number such as "8.0" or "2.7.5". Alternatively, the version may be specified as a Git branch/revision pair in the form ``<branch>@<revision>``. For example "main@36ba65jg53.....". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again, this version must be a version number corresponding to an official GeoTools release.

**GWC_VERSION**

  The GeoWebCache version to include in the release. This may be specified as a version number such as "1.3-RC3". Alternatively, the version may be specified as a Git revision of the form ``<branch>@<revision>`` such as "master@1b3243jb...". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT).Git Again, this version must be a version number corresponding to an official GeoTools release.

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
test on the developer list.

It is important to test the artifacts using the minimum supported version of Java (currently Java 11 in September 2023).

Publish the Release
-------------------

Run the `geoserver-release-publish <https://build.geoserver.org/view/geoserver/job/geoserver-release-publish/>`_ in Jenkins. The job takes the following parameters:

**VERSION**

  The version being released. The same value specified for ``VERSION`` when running the ``geoserver-release`` job.

**BRANCH**

  The branch being released from.  The same value specified for ``BRANCH`` when running the ``geoserver-release`` job.

This job will rsync all the artifacts located at::

     http://build.geoserver.org/geoserver/release/<RELEASE>

to the SourceForge FRS server. Navigate to `SourceForge <http://sourceforge.net/projects/geoserver/>`__ and verify that the artifacts have been uploaded properly. If this is the latest stable release, set the necessary flags on the ``.exe``, ``.dmg`` and ``.bin`` artifacts so that they show up as the appropriate default for users downloading on the Windows, OSX, and Linux platforms.

Release notes
-------------

This job will tag the release located in::
   
   https://github.com/geoserver/geoserver/tags/<RELEASE>

Publish JIRA markdown release notes to GitHub tag:

#. Select the correct release from `JIRA Releases <https://osgeo-org.atlassian.net/projects/GEOS?orderField=RANK&selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page&status=released>`__ page.

#. From the release page, locate the :guilabel:`Release notes` button at the top of the page to open the release notes edit
  
#. Generate release notes as markdown:
   
   * Select format `Markdown`
   * Layout: Issue key with link
   * Issue types: `Bug` and `Improvement`
   
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
   
   * Release title: `GeoServer 2.20.0`
   * Write: Paste the markdown from Jira release notes editor
   
   Use :guilabel:`Publish release` button to publish the release notes.
   
Create the download page
------------------------

The `GeoServer website <http://geoserver.org/>`_ is managed as a `GitHub Pages repository <https://github.com/geoserver/geoserver.github.io>`_. Follow the `instructions <https://github.com/geoserver/geoserver.github.io#releases>`_ in the repository to create a release announcement.

The announcement page header fields include the information required to generate a download page for the release. 

Announce the Release
--------------------

Mailing lists
^^^^^^^^^^^^^

.. note:: This announcement should be made for all releases, including release candidates.

Send an email to both the developers list and users list announcing the
release. The message should be relatively short. You can base it on the blog post.
The following is an example::

   Subject: GeoServer 2.5.1 Released

   The GeoServer team is happy to announce the release of GeoServer 2.5.1.
  
   The release is available for download from:

   http://geoserver.org/release/2.5.1/

   GeoServer 2.5.1 is the next stable release of GeoServer and is recommended for production deployment.

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
   * This release is made in conjunction with GeoTools 11.1
   
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

