.. _time_boxed_releases:

Time boxed releases
===================

The release model, starting with GeoServer 2.2.0, is based on time boxing, that is, a setup in which
the software is released:
* at predictable dates with whatever
* with whatever fix/improvements are available at the time

To compensate the eventual unpredictability of the release contents the model includes strict rules
about what might be committed on each branch, and a suitably long hardening period in which the
unstable series gets stabilized.

Release timings
---------------

The cycle is based on:
* monthly releases on the stable series
* a four month open development period followed by two months hardening period 
* beta releases are supposed to be released out of the unstable series on a monthly basis
  across the switch between open development and hardening, followed by the first RC
* RC are pushed out every two weeks until we reach a stable code base, which will be released
  as the new major stable release
* the first RC marks the branch off of a new trunk, on which open development starts again

The following picture exemplifies the cycle:

 .. image:: timeboxed.png 

Every month, on the same day, a new release is issued using whatever revision of GeoServer/Geotools passed the last CITE tests.
The release is meant to improve upon the previous release in both functionality and stability, so unless 
the project steering committee determines reasons to block the release it will happen regardless of what bug 
reports are in Jira (pending resourcing, they can be fixed in the next release that comes out one month later).

At every point in time there are two branches, a stable branch and a trunk, with just one month every 
six where there are three active branches (nothing prevents developers willing to keep the stable series 
up longer working there if they wish to, it's just not expected anymore).

The three phases
----------------

Stable branch
`````````````

The stable branch is meant for bug fixes and new features that do not affect the GeoServer API or 
significantly affect the stability.
A PSC vote (with eventual proposal) can be called in case a significant new feature or change needs 
to be back ported to the stable branch overriding the above rules.

If, for any reason, a release is delayed the next release will be rescheduled 30 days after the last release
(that is, delaying the whole training of remaining releases).

Trunk in open development mode
``````````````````````````````

The open development mode starts when the new stable release is branched off, and ends when hardening
starts, four months after the new stable release is made.

During this operational mode developers are free to commit stability undermining changes (even significant ones). 
Those changes still need to be voted as GSIP anyways to ensure, as usual, resourcing checks, API consistency and community review.

After three months from the release of the stable series a first beta will be released, 
one month after that the second beta will be released and the trunk will switch into hardening mode.

Trunk in hardening mode
```````````````````````

The harderning mode starts when the second beta is released and continuous through all release candidate
releases. The first RC is released one month after the second beta, and then bi-weekly releases
will be issued until no major issues will be reported by the user base, at which point the last RC
will be turned into the new stable release.

During harderning only bug-fixes and new non core plugins can be developed

Commit rules
------------

While the PSC is going to be able to vote and override the committing guidelines, it is still good 
to have some reference and default of what can be done or not done in the various branches.

**Hardening mode**, and by extension, stable and trunk, can take any of the following:

* bug fixes
* documentation improvements
* new plugins contributed as community or extension modules (with a cautionary note that during 
  hardening the attention should be concentrated as much as possible on getting the new release stable)


**Stable branch** can take in addition the following:

* new minor core functionality (e.g. new output format,
* new API that we can commit to for a long period of time (provided it's not a change to existing API unless the PSC votes otherwise). 
  GeoServer is not a library, so defining API can be hard, but any class that can be used by pluggable 
  extension points should be changed with care, especially so in a stable series

In addition to the above the **stable branch** can get the following changes provided there is
a **solid PSC/PMC vote** (e.g., no doubts or concerns about them) to include them:

* promotion of extensions to core
* core changes that are unlikely to affect the stability of the upcoming release 
  (if the PSC is ok better land them right after a release to get as a large window for testing as possible)
* backport of larger changes that have proven to be working well on trunk for an extended period of time

**Trunk** in open development mode can take everything, of course large changes are still subject to proposals and reviews.