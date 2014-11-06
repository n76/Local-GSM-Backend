GSM LocationProvider backend for µg UnifiedNlp
==============================================

The µg Project at https://github.com/microg has a unified network location provider that accepts backends for the actual location lookup. This project implements a cell-tower based lookup for your current location.

This "GSM Location Backend" works without network connectivity and will
never post your data anywhere.

The on-phone database used to look up cell tower locations can be generated from scripts at https://github.com/n76/lacells-creator Those scripts gather tower information from OpenCellId http://opencellid.org and Mozilla Location Services https://location.services.mozilla.com

You can improve the quality of the data used by this location provider by contributing to either or both of those projects.

This software is licensed as "Apache License, Version 2.0" unless noted
otherwise.

This software is derived from LocalGSMLocationProvider at https://github.com/rtreffer/LocalGSMLocationProvider but differs as follows:

1. This software uses a sqlite database rather than the custom built one in LocalGSMLocationProvider.
2. This software does not ship with a built in database. Databases can be generated with the scripts at https://github.com/n76/lacells-creator
3. The database used by this software includes an estimated range of coverage for each cell tower. That information is used to provide a location estimate weighted by coverage area and to make an estimate of position accuracy. (The original LocalGSMLocationProvider assumes 800 meter range for all towers.)

Requirement for building
========================

1. Android development platform

Requirements on phone
=====================
1. This is a plug in for µg UnifiedNlp which can be installed from f-droid.

How to build and install
=======================-

1. ant debug
2. adb install bin/android_apps_gsmlp-debug.apk

Setup on phone
==============
1. Install a cell tower database. The scripts at https://github.com/n76/lacells-creator can be used to build and install a database.
2. In the NLP Controller app (interface for µg UnifiedNlp) select the "GSM Location Backend".
