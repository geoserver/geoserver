.. _review:

Review
======

Reviewing code is an invaluable process that is used to ensure that code meets an acceptable level of quality prior to being included in a GeoServer release.

Informal Code Review
--------------------

For GeoServer developers there is no hard policy relating to code review such that every line of code needs to be reviewed, over time we have adopted the following practice:

* if the change is to a core module and the developer does not have core commit access it requires a pull request and formal review
* if the change is non-trivial and to a core module, it should be be handed as a pull-request
* if the change is local to a module the developer maintains it usually does not require a review
* community modules are a playground for experimentation - collaborate with the module maintainer as appropriate

In general a code review is always welcome and if there is a question as to whether a change should be reviewed or not, always err on the side of getting it reviewed.

Pull Request Review Guide
-------------------------

This guide provides guidance to help pull requests be reviewed in a consistent fashion (and help ensure we do not forget anything).

Before you start:

* Double check the github [CONTRIBUTING](https://github.com/geoserver/geoserver/blob/master/CONTRIBUTING.md) policy capturing requirements for submitted code.

  This document is shown to contributors each time a pull request is made.
  
* Be clear when reviewing between directives (iso speak MUST,FIX,SHALL) and suggestions or ideas for improvements (iso speak SHOULD, MAY, IDEA).

* As a reviewer we must balance between not having enough time for thorough review, and the desire to provide quick feedback.

Initial Checks
^^^^^^^^^^^^^^

The following checks can be performed quickly and represent common mistakes or oversites. These quick checks can be performed by anyone as an initial inspection.

.. note:: Providing an initial check, while better than no follow up at all, may seem petty and discouraging to the contributor. It is a good idea to state that this is only quick feedback - while either more time or an appropriate expert is found.

* *Contribution agreement* on file. Check if the contribution requires a contribution agreement, and if it matches the requisites, verify that a contribution agreement has been provided to OSGeo, if not, demand for one.

* *Presence of tests*. Given a large code base, that pretty much nobody really can hope to master fully, the large number of external contributors, as well as focused changes to the code base, and the fast evolution of the code base, tests are really the only line of defense against accidental breakage of the contributed functionality. That is why we always demand to have at least one test, it's not a "punishment", but a basic guarantee that the fix or new functionality being contributed will still be there, and working, a bare few months later. So always check there is at least one test, one that would break in a evident way if there ever was a regression in the contributed code.

* *Presence of documentation*. For functional, user or protocol facing changes, check that some basic documentation has been contributed. Almost nobody will know functionality is there if we don't have documentation on how it works. As a plus, documentation will help you understand quicker what is getting contributed, both from a high level view, and as a guide though the changes being made.

* *Presence of a proposal*, if required. For any large change our process demands a formal proposal to be made and discussed in the community before a pull request gets made. If the pull request is made anyways, point them to the process and warn that the community might eventually request changes in the approach

* *Copyright headers* are present. Every source file needs a copyright header to be present. For new files ask the contributor to add the header. Copyright headers do not need to be updated for changes to existing files.

* *No commented out code sections*. The version control is able to tell differences between existing and past versions of a certain file, thus, the commit should not contain commented out code sections.

* *Javadocs and comments*. Public classes and methods should have a minimum of javadoc (a simple sentence explaning what the method or class does will be sufficient), difficult parts of the code should have some comments explaining what the code does (how it does it, is evident by the code). Comments should be professional (no personal opinions or jokes), current to the code (no need to explain what was there before, unless there is a high risk of it coming back), with no reference to the comment author (in case we need to know that information, a git blame will do)

* *Code formatting*. The project has code formatting guidelines embodied in a Eclipse code formatting template, for every other IDEs it's basically the official Java coding conventions (spaces instead of tabs) with "long" lines.

* *Reformats and other changes obfuscating the actual patch*. We recommend contributors to limit changes to a minimum and avoid reformatting the code, even if the existing code is not following the existing coding conventions. Reformats can be put in separate commits if necessary.

Extensive Review
^^^^^^^^^^^^^^^^

The key point of a review is to make sure that GeoServer remains stable (does not regress in behaviour) and that the codebase remains flexible and maintainable into the future.

.. note:: This review often requires some experience with the code being modified, and will require more time and focus than the initial check described in the previous section.

* *Backwards compatibility*. The change being proposed should not hamper backwards compatibility, every change must be performed ensuring that users can keep on upgrading the GeoServer behind their services and application without functional regressions.
* *Standards conformance*. The change being proposed must not be at odds with GeoServer OGC standard conformance, in particular, after the change the CITE tests must keep on passing properly, and there should be no visible violation of known standards. This is not to say that we cannot occasionally break compliance, but that doing so must be controlled by a configuration flag that the admin chooses to enable, and that by default, the flag is turned off. Extensions to the standard are also welcomed, but are better discussed on the list and setup in such a way that they look like a natural extension.
* *Performance*. The change should not introduce evident performance regressions. This is not to say that every pull request must be load tested, but some attention should be paid during the review to changes that might be damaging in those respects, looking for CPU hungry code or heavy memory allocation
* *Leaks*. A java server side application like GeoServer should be able to run for months uninterrupted, thus particular attention should be paid to resource control, in particular resources that ought to to closed (files, connections, pools in general), significant allocation of classes with finalizers.
* *Thread safety*. GeoServer is, like all Java server side application, serving one request per thread. In this respect thread safety is of paramount importance. Be on the lookout for lazy initialization, stateful classes shared among threads, thread locals that fail to be properly cleaned at the end of the request, and static fields and data structures in general.
* *Good usage and fit with the existing code and architecture*. The code is easier to understand and maintain when it follows common pattern across the code base, and when there is little or no code duplication. Check the pull request for conformance with the existing code, and proper usage of existing facilities.
* *Use of the Resources (not Files)*. Contributors should read and write to resources using the ResourceStore API whenever applicable and only convert Resources to Files when absolutely necessary (for example, for a third party library).
* *Proper module usage*. There is often a strong temptation to put new functionality in core as opposed to a new community module. If this is the case, verify the functionality is indeed core worthy, that is, relevant for many users, properly documented, has core developers interested in maintaining it long term, and heavily tested.
* *IP checks*. When there is evidence that some of the code is coming from a different code base, check the contributor actually has the rights to donate it to GeoServer, and that the original licence is compatible (or that the author owns the code, and can thus relicense it under the GPL terms).
* *Current Java version and library usage*. Check the new code uses the current version of Java (e.g. foreach, try with resources, generics, lambdas), and current library facilities (JUnit, Spring) instead of using outdated structures, rolling its own replacements or adding new dependencies. Attention should be paid to patterns that while elegant, might incur in significant overhead in performance sensitive areas of the code (e.g., arrays vs collection, inheritance and overridden methods, and other forms of abstraction above the "bare metal").
* *Malicious code*. While unlikely, a pull request might contain malicious code to create, by design or accident, openings in the security of GeoServer that an external attacker might use. Attention should be paid to input checks, XML expansion attacks, reflection though serialization (which can be used to generate a remote execution attack).