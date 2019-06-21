.. _release_guide:

Release Guide
=============

This guide details the process of performing a GeoServer release.

Before you start
----------------

SNAPSHOT release
^^^^^^^^^^^^^^^^

For any release (including release candidates) a GeoServer release requires an
corresponding GeoTools and GeoWebCache release. Therefore before you start you should
coordinate a release with these projects. Either performing the release yourself or
asking a volunteer to perform the release.

* `GeoTools Release Guide <http://docs.geotools.org/latest/developer/procedures/release.html>`_
* `GeoWebCache RELEASE_GUIDE.txt <https://github.com/GeoWebCache/geowebcache/blob/master/geowebcache/release/RELEASE_GUIDE.txt>`_

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

When performing a release we don't require a "code freeze" in which no developers can commit to the repository. Instead we release from a revision that is known to pass all tests, including unit/integration tests as well as CITE tests.

To obtain the GeoServer and Geotools revisions that have passed the `CITE test <https://build.geoserver.org/view/testing-cite/>`_, navigate to the latest Jenkins run of the CITE test  and view it's console output and select to view its full log. For example:

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

When creating the first release candidate of a series, there are some extra steps to create the new stable branch and update the version on master.

* Checkout the master branch and make sure it is up to date and that there are no changes in your local workspace::

    git checkout master
    git pull
    git status

* Create the new stable branch and push it to GitHub; for example, if master is ``2.11-SNAPSHOT`` and the remote for the official GeoServer is called ``geoserver``::

    git checkout -b 2.11.x
    git push geoserver 2.11.x

* Enable `GitHub branch protection <https://github.com/geoserver/geoserver/settings/branches>`_ for the new stable branch: tick "Protect this branch" (only) and press "Save changes".

* Checkout the master branch and update the version in all pom.xml files; for example, if changing master from ``2.11-SNAPSHOT`` to ``2.12-SNAPSHOT``::

    git checkout master
    find . -name pom.xml -exec sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' {} \;

  .. note:: ``sed`` behaves differently on Linux vs. Mac OS X. If running on OS X, the ``-i`` should be followed by ``'' -e`` for each of these ``sed`` commands.

* Update release artifact paths and labels, for example, if changing master from ``2.11-SNAPSHOT`` to ``2.12-SNAPSHOT``::

    sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/bin.xml
    sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/installer/win/GeoServerEXE.nsi
    sed -i 's/2.11-SNAPSHOT/2.12-SNAPSHOT/g' src/release/installer/win/wrapper.conf

  .. note:: These can be written as a single ``sed`` command with multiple files.

* Update GeoTools dependency; for example if changing from ``17-SNAPSHOT`` to ``18-SNAPSHOT``::

    sed -i 's/17-SNAPSHOT/18-SNAPSHOT/g' src/pom.xml

* Update GeoWebCache dependency; for example if changing from ``1.11-SNAPSHOT`` to ``1.12-SNAPSHOT``::

    sed -i 's/1.11-SNAPSHOT/1.12-SNAPSHOT/g' src/pom.xml

* Manually update hardcoded versions in configuration files:

    * ``doc/en/developer/source/conf.py``
    * ``doc/en/docguide/source/conf.py``
    * ``doc/en/user/source/conf.py``
    * ``doc/es/user/source/conf.py``
    * ``doc/fr/user/source/conf.py``

* Commit the changes and push to the master branch on GitHub::

      git commit -am "Updated version to 2.12-SNAPSHOT, updated GeoTools dependency to 18-SNAPSHOT, updated GeoWebCache dependency to 1.12-SNAPSHOT, and related changes"
      git push geoserver master
      
* Create the new RC version in `JIRA <https://osgeo-org.atlassian.net/projects/GEOS>`_ for issues on master; for example, if master is now ``2.12-SNAPSHOT``, create a Jira version ``2.12-RC1`` for the first release of the ``2.12.x`` series

* Update the main, nightly, geogig-plugin and live-docs jobs on build.geoserver.org:
  
  * disable the maintenance jobs, and remove them from the geoserver view
  * create new jobs, copying from the existing stable jobs, and edit the branch.
  * modify the last line of the live-docs builds, changing ``stable`` to ``maintain`` for the previous stable branch. The new job you created should publish to ``stable``, and master will continue to publish to ``latest``.

* Update the cite tests on build.geoserver.org:

  * disable the maintenance jobs, and remove them from the geoserver view
  * create new jobs, copying from the existing master jobs, editing the branch in the build command.

* Announce on the developer mailing list that the new stable branch has been created.

* Switch to the new branch and update the documentation links, replacing ``docs.geoserver.org/latest`` with ``docs.geoserver.org/2.12.x`` (for example):
   
  * ``README.md``
  * ``doc/en/developer/source/conf.py``
  * ``doc/en/user/source/conf.py``

Build the Release
-----------------

Run the `geoserver-release <https://build.geoserver.org/view/geoserver/job/geoserver-release/>`_ job in Jenkins. The job takes the following parameters:

**BRANCH**

  The branch to release from, "2.2.x", "2.1.x", etc... This must be a stable branch. Releases are not performed from master.

**REV**

  The Git revision number to release from. eg, "24ae10fe662c....". If left blank the latest revision (ie HEAD) on the ``BRANCH`` being released is used.

**VERSION**

  The version/name of the release to build, "2.1.4", "2.2", etc...

**GT_VERSION**

  The GeoTools version to include in the release. This may be specified as a version number such as "8.0" or "2.7.5". Alternatively the version may be specified as a Git branch/revision pair in the form ``<branch>@<revision>``. For example "master@36ba65jg53.....". Finally this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again, this version must be a version number corresponding to an official GeoTools release.

**GWC_VERSION**

  The GeoWebCache version to include in the release. This may be specified as a version number such as "1.3-RC3". Alternatively the version may be specified as a Git revision of the form ``<branch>@<revision>`` such as "master@1b3243jb...". Finally this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT).Git Again, this version must be a version number corresponding to an official GeoTools release.

**GIT_USER**

  The Git username to use for the release.

**GIT_EMAIL**

  The Git email to use for the release.

This job will checkout the specified branch/revision and build the GeoServer
release artifacts against the GeoTools/GeoWebCache versions specified. When
successfully complete all release artifacts will be listed under artifacts in the job summary.

Additionally when the job completes it fires off a job for a windows worker. When this job
completes it will list the ``.exe`` artifacts.

Test the Artifacts
------------------

Download and try out some of the artifacts from the above location and do a
quick smoke test that there are no issues. Engage other developers to help
test on the developer list.

Publish the Release
-------------------

Run the `geoserver-release-publish <https://build.geoserver.org/view/geoserver/job/geoserver-release-publish/>`_ in Jenkins. The job takes the following parameters:

**VERSION**

  The version being released. The same value s specified for ``VERSION`` when running the ``geoserver-release`` job.

**BRANCH**

  The branch being released from.  The same value specified for ``BRANCH`` when running the ``geoserver-release`` job.

This job will rsync all the artifacts located at::

     http://build.geoserver.org/geoserver/release/<RELEASE>

to the SourceForge FRS server. Navigate to `Sourceforge <http://sourceforge.net/projects/geoserver/>`__ and verify that the artifacts have been uploaded properly. If this is the latest stable release, set the necessary flags on the ``.exe``, ``.dmg`` and ``.bin`` artifacts so that they show up as the appropriate default for users downloading on the Windows, OSX, and Linux platforms.

Create the download page
------------------------

The `GeoServer web site <http://geoserver.org/>`_ is managed as a `GitHub Pages repository <https://github.com/geoserver/geoserver.github.io>`_. Follow the `instructions <https://github.com/geoserver/geoserver.github.io#releases>`_ in the repository to create a download page for the release. This requires the url of the blog post announcing the release, so wait until after you have posted the announcement to do this.

Post the Documentation
----------------------

.. note:: For now, this requires Boundless credentials; if you do not have them, please ask on the `GeoServer developer list <https://lists.sourceforge.net/lists/listinfo/geoserver-devel>`_ for someone to perform this step for you.

.. note:: This content will likely move to GitHub in the near future.

#. Log in to the server.

#. Create the following new directories::

     /var/www/docs.geoserver.org/htdocs/a.b.c
     /var/www/docs.geoserver.org/htdocs/a.b.c/developer
     /var/www/docs.geoserver.org/htdocs/a.b.c/user

   where ``a.b.c`` is the full release number.

#. Download the HTML documentation archive from the GeoServer download page, and extract the contents of both user manuals to the appropriate directory:
    
    .. code-block:: bash

       cd /var/www/docs.geoserver.org/htdocs/a.b.c/
       sudo wget http://downloads.sourceforge.net/geoserver/geoserver-a.b.c-htmldoc.zip
       sudo unzip geoserver-a.b.c-htmldoc.zip
       sudo rm geoserver-a.b.c-htmldoc.zip

   .. note:: Steps 2 and 3 have now been automated by a bash script on the server, and can be completed by executing:
      
      .. code-block:: bash
         
         sudo /var/www/docs.geoserver.org/htdocs/postdocs.sh a.b.c
 
#. Open the file :file:`/var/www/docs.geoserver.org/htdocs/index.html` in a text editor.

#. Add a new entry in the table for the most recent release::

    <tr>
      <td><strong><a href="http://geoserver.org/release/a.b.c/">a.b.c</a></strong></td>
      <td><a href="a.b.c/user/">User Manual</a></td>
      <td><a href="a.b.c/developer/">Developer Manual</a></td>
    </tr>

#. Save and close this file.

Announce the Release
--------------------

GeoServer Blog
^^^^^^^^^^^^^^

.. note:: This announcement should be made for all releases, including release candidates.

.. note::

   This step requires an account on http://blog.geoserver.org/

#. Log into the `GeoServer Blog <http://blog.geoserver.org/wp-login.php>`_.

#. Create a new post. The post should be more "colorful" than the average
   announcement. It is meant to market and show off any and all new
   features.

   .. code-block:: html

      The GeoServer team is pleased to announce the release of
      <a href="http://geoserver.org/release/2.5.1/">GeoServer 2.5.1</a>:
      <ul>
         <li>Downloads (<a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1-bin.zip/download">zip</a>,
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1-war.zip/download">war</a>,
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1.dmg/download">dmg</a> and
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1.exe/download">exe</a>) are listed on the
             <a href="http://geoserver.org/release/2.5.1/">GeoServer 2.5.1</a> page
             along with documentation and extensions.
            <ul>
               <li>This release includes and is made in conjunction with
                 <a href="http://geotoolsnews.blogspot.com/2014/05/geotools-111-released.html">GeoTools 11.1</a>.</li>
            </ul>
         </li>
         <li>Thanks to <a href="http://www.warwickshire.gov.uk/">Warwickshire County Council</a>
             for some great GeoWebCache integration work:
            <ul>
               <li>GeoWebCache tile layer HTTP cache headers are now taken from GeoServer layer configration</li>
               <li>GeoWebCache settings are now correctly saved on Windows</li>
            </ul>
         </li>
         <li>A wide range of improvements provided by the community
            <ul>
               <li>Scale hints now exposed in WMS GetCapabilities document</li>
               <li>Fixed Symbology Encoding 1.1 encoding of relative external graphics</li>
               <li>Addressed axis order issues cascading WMS 1.3.0 services through GeoServer</li>
            </ul>
         </li>
      </ul>
      More details can be found in the
      <a href="https://osgeo-org.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=10164">GeoServer 2.5.1 Release Notes</a>.

#. Examples of content:

   * Link to the **Download Page** in the wiki created above, and possibly to the
     installers for each platform.

     Example: `GeoServer 2.3.4 Released <http://blog.geoserver.org/2013/07/28/geoserver-2-3-4-released/>`_

   * Indicate which version of GeoTools is used, and thank your employer.

   * Link to completed pull requests and Jira tickets, looking for new features or
     important bug fixes to highlight. Make a point to thank new contributors
     and sponsors.

     Example: `GeoServer 2.3.1 released <http://blog.geoserver.org/2013/04/23/geoserver-2-3-1-released/>`_

   * For the run up to a major release you can build up a list of the new features and
     change requests.

     Example: `GeoServer 2.4 Beta Released <http://blog.geoserver.org/2013/07/22/geoserver-2-4-beta-released/>`_

   * For the major release you can spend a bit more time on the new features, linking
     to blog posts if they are available.

     Example: `GeoServer 2.3-beta released <http://blog.geoserver.org/2013/01/29/geoserver-2-3-beta-released/>`_

#. Do not publish the post right away. Instead ask the devel list for review.


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

   GeoServer 2.5.1 is the next the stable release of GeoServer and is recommended for production deployment.

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

OSGeo Anouncement
^^^^^^^^^^^^^^^^^

For major releases OSGeo asks that a news item be submitted:

* Login to the osgeo.org website, create a news item using the release announcement text above.

And that an announcement is sent to discuss:

* Mail major release announcements to discuss@osgeo.org (you will need to `subscribe first <https://lists.osgeo.org/listinfo/discuss>`__ ). 

