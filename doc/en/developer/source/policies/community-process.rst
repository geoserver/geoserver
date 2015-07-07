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

* **Balance**
  Every member of the community is welcome to participate. Project decisions
  are discussed in public by the community as a whole. Decisions are made by a project steering committee and not subject to the whims of any single developer or organizations.
  
* **Responsibility**
  The Project Steering Committee is responsible the strategic direction of the GeoServer project. Central to this responsibility is evaluating proposals and providing a clear decision through a public voting process.

Adding features
^^^^^^^^^^^^^^^

During the development cycle community members propose new features and improvements to 
be included in the release. The following are the prerequisites for proposing a 
new feature:

#. The feature has a sponsor. This means either a developer willing to carry out
   the work or a customer who is paying for it.
#. The feature has has gone through the :ref:`GSIP <gsip>` process 
   **if necessary**. Whether a feature requires a GSIP is decided by the 
   community when the feature is proposed.

The determining factor for what release a feature should be included in is based on the estimate of the time to implement the feature, and the current :ref:`release_cycle`. The release cycle includes a "feature freeze" where new features are delayed while stabilize master and cut a new release candidate.

New features may be back-ported to the stable series (if technically feasible) after being tried out on master for a month.

.. _release_cycle:

Release cycle
^^^^^^^^^^^^^

GeoServer follows a time-boxed release model, this allows new features to be developed for the project with a set expectation of when a feature will be available for use.

The community maintains three active branches:

* master: available for development of new functionality, documentation improvements and bug fixes
* stable: bug fixes and back-port of new functionality that do not affect the GeoServer API or significantly affect stability.
* maintenance: bug fixes

GeoServer uses a six month development cycle on the master branch:

  * month 1-5 master is open for development
  * month 5 a beta release is made on a feature freeze
  * Month 6 a release candidate is made, and a new master is created ending the feature freeze

Followed by a year of production support with six months of stable releases and six months of maintenance releases:
 
  * Month 1 initial stable release 
  * Month 3 stable release
  * Month 5 stable release
  * Month 7 maintenance release
  * Month 9 maintenance release
  * month 11 maintenance release

We alternate between releasing the stable and maintenance branches so a release goes out each month.
