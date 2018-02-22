.. _community_process:

Community Process
=================

This document describes the process that the GeoServer community uses to handle
contributions and additions to the project. This process is used to manage 
new features and additions to the project, as well as for short and long term 
project planning.

The GeoServer community process adheres to the following guidelines to ensure 
that it retains open development practices:

* **Transparency** 
  Project decisions are made in an open and transparent way. Key decisions are presented *GeoServer Improvement Proposals* (:ref:`GSIP <gsip>`), discussed in public. When a decision is made it is clear as to why it was made.

* **Open**
  Proposals are presented on the `GeoServer wiki <https://github.com/geoserver/geoserver/wiki>`__ - and anyone can write a proposal (no commit access required).
  
* **Balance**
  Every member of the community is welcome to participate. Project decisions
  are discussed in public by the community as a whole. Decisions are made by a project steering committee and not subject to the whims of any single developer or organizations.
  
* **Responsibility**
  The Project Steering Committee is responsible the strategic direction of the GeoServer project. Central to this responsibility is evaluating proposals and providing a clear decision through a public voting process.

Adding features
^^^^^^^^^^^^^^^

During the development cycle community members propose new features and improvements to 
be included in the next release. The following are the prerequisites for proposing a 
new feature:

#. The feature has a sponsor. This means either a developer willing to carry out
   the work or a customer who is paying for it.
#. The feature has has gone through the :ref:`GSIP <gsip>` process 
   **if necessary**. Whether a feature requires a GSIP is decided by the 
   community when the feature is proposed.

The determining factor for what release a feature should be included in is based on the estimate of the time to implement the feature, and the current :ref:`release_cycle`.

New features may be back-ported to the stable series (if technically feasible) after being tried out on master for a month.

Adding fixes
^^^^^^^^^^^^

During the release cycle community members contribute fixes to be included, and backported, to be included in subsequent releases. 

#. Each fix requires an issue tracker entry, to be included in the release notes
#. Each fix must be applied to the master branch, and then back ported.
#. While a release may be held for a "blocking" issue this is determined by discussion on the developer email list.

Please respect our release volunteers. We stop back porting fixes the day before release so CITE tests can verify the release includes all the changes needed.

.. _release_cycle:

Release cycle
^^^^^^^^^^^^^

GeoServer follows a time-boxed release model, this allows new features to be developed for the project with a set expectation of when a feature will be available for use.

The community maintains three active branches:

* master: available for development of new functionality, documentation improvements and bug fixes
* stable: bug fixes and back-port of new functionality that do not affect the GeoServer API or significantly affect stability.
* maintenance: bug fixes

For each GeoServer release we spend six month "prerelease" in a development cycle on the master branch, followed by six months as the stable release, followed by six months as the maintenance release.

..note:: The former beta release has been replaced with an earlier release candidate. There is no longer a "feature freeze" on master after this release. Instead, the new branch is created at this time, freeing up master for new features.

**Prerelease**

  * Month -6: master open for development
  * Month -1: month:  release candidate is made on new branch
  * Month 1: (start of month): second release candidate is made, if there are sufficient changes to warrant it.

**Release**
   
  * Month 1: initial stable release (aim for one month after the first release candidate)
  * Month 3: stable release
  * Month 5: stable release
  * Month 7: maintenance release
  * Month 9: maintenance release
  * Month 11: maintenance release

We alternate between releasing the stable and maintenance branches. A release goes out each month forming a yearly release cycle.

.. figure:: release-cycle.png
   
   GeoServer 2.8 Release Cycle

Here is what that looks like:

  * Month 1: Release N.0 stable 
  * Month 2: (previous branch N-1 issues a maintenance release)
  * Month 3: Release N.1 stable
  * Month 4: (previous branch N-1 issues a maintenance release)
  * Month 5: Release N.2 stable
  * Month 6: (next branch N+1 issues a stable release)
  * Month 7: Release N.3 maintenance
  * Month 8: (next branch N+1 issues a stable release)
  * Month 9: Release N.4 maintenance
  * Month 10: (next branch N+1 issues a stable release)
  * Month 11: Release N.5 maintenance

For more information, or to volunteer, please check the `release schedule <https://github.com/geoserver/geoserver/wiki/Release-Schedule>`__ in the wiki.

**Unscheduled Releases**

Additional releases may be requested by downstream projects at any point, or may be produced by a volunteer to quickly disseminate a security fix.

* Additional stable (or maintenance releases) will use the next available version number. This does not disrupt the release schedule above. We expect volunteers to use common sense and collaborate rather than issue two releases during the same week.
* Patch releases are formed by branching from a previous release tag, applying a fix, and issuing a release. Patch releases are versioned appropriately.
  
  As an example GeoServer 2.5.5.1 is a patch release started by branching the GeoServer 2.5.5.


