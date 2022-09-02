.. _pull_request:

Pull Requests
=============

As with most open source project GeoServer is very happy to accept pull requests from the community. Contributions from the community have been a source of some of the best bug fixes and improvements in GeoServer and is a great way to give back to the project.

This document is a guide designed to help contributors in successfully submitting a pull request:

.. note:: 

   We ask that all fixes applied to the main development branch first, and then backported to the stable and maintenance branches.
   
   We require the following for fixes submitted via pull requests:

   1. Required: Prepare your fix for the main development branch
   2. Required: Check the following (and comment in your pull request, or jira issue):
      
      * Investigate if the issue impacts any of the other active branches (the main development branch / stable / maintenance)
      * Investigate whether the fix can be backported to the applicable active branches (the main development branch / stable / maintenance)
      
   3. Recommended: Actual backport to stable and maintenance is optional but highly recommended.

   We require the following for new features submitted via pull request or patch:
   
   1. Required: Prepare your feature for the main development branch.
   2. Optional: If the new feature is suitable for backport, you may ask on the developer list after a 1 month.

GitHub pull requests
--------------------

The GeoServer git repository is hosted on GitHub, which manages contributions in the form of `pull requests <https://help.github.com/articles/using-pull-requests/>`_.  You will need to `fork the GeoServer git repo <https://github.com/geoserver/geoserver/fork_select>`_ into your own account to create a pull request.

See the :ref:`developer quickstart <quickstart>` for information on forking and building your own copy of GeoServer from source.

Assuming that ``origin`` points to your GitHub repo, the patch workflow is as follows:

#. Make the change.::

     git checkout -b my_bugfix main
     git add .
     git commit -m "fixed bug xyz"

#. Push the change up to your GitHub repository.::

     git push origin my_bugfix

#. Visit your GitHub repo page and create the pull request. 

#. For the release notes, we ask that you open a `JIRA <https://osgeo-org.atlassian.net/projects/GEOS>`_ ticket linking to your pull request.

   At this point the core developers will be notified of the pull request and review it at the earliest convenience.

Core developers will :ref:`review` the patch and may require changes or improvements to it prior to it being accepted. It will be up to the submitter to amend the pull request, actively revising until it gets merged. Please be patient as pull requests are often reviewed during maintainers' spare time so turn-around can be a little slow.

If a pull request becomes stale with no feedback from the submitter for a couple of months, it will linked form a JIRA issue (to avoid losing the partial work) and then be closed.

Tips and Tricks
---------------

The following guidelines are meant to ensure that contirbutions submitted via pull request will be as easy as possible to review and merge.

Clean Build
^^^^^^^^^^^

In general developers will only accept pull requests that apply cleanly against the latest versions of the stable and the main development branches. Therefore before you generate a patch it is important that you update your checkout to the latest for the branch you are working on.

Ensure your IDE/editor is properly configured
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ensure that your development environment is properly configured for GeoServer development. A common issue 
with patches from newcomers is that their text editor is configured to use tabs rather than spaces.

See the :ref:`eclipse_guide` for general information about formatting and IDE setup. 

Include only relevant changes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ensure the patch only contains changes relevant to the issue you are trying to fix. A common mistake is 
to include whitespace and formatting changes along with the relevant changes. These changes, while they 
may seem harmless, make the patch much harder to read.

Fix one thing at a time
^^^^^^^^^^^^^^^^^^^^^^^

Do not batch up multiple unrelated changes into a single patch. If you want to fix multiple issues work
on them separately and submit separate patches for them.

Be patient
^^^^^^^^^^

The core developers review community patches in spare time. Be cognizant of this and realize that just 
as you are contributing your own free time to the project, so is the developer who is reviewing and 
applying your patch.

We understand professionals working on the code base may need to meet customer or contractual obligations. You may wish to review :website:`commercial support <support>` options if you are not in a position to wait for a volunteer. We ask that professionals take an extra level of pride in their work, it is embarrassing to make your customers wait over common mistakes. 

Test Case
^^^^^^^^^

Include a test case that shows your patch fixes an issue (or adds new functionality). If you do not include a test case, the developer reviewing your work will need to create one.

Issue Tracker
^^^^^^^^^^^^^

`JIRA Issue <https://osgeo-org.atlassian.net/projects/GEOS>`_ are used to list your fix in the release notes each release. You can link to the JIRA ticket in your pull request description.

