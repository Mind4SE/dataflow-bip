dataflow-bip
============

The Dataflow BIP plugin developed during the Minalogic collaborative project, by Verimag and ST Microelectronics.

What to expect from this repository
-----------------------------------

- Plugins

This current version plugins have been adapted to work with the 2.1-SNAPSHOT toolchain.

- Examples

"helloworld-simple" and "helloworld-beam-fork" have been adapted to work (some minor issues were encoutered) to test the evolutions.

- Library

Some fixes were done in the beam-comp-library to make the examples work (missing @BeamInterface annotations...)

Limitations
-----------

The "beamc" assembly project now creates an archive with the "beam-backend" and "beam-frontend" modules in the "ext" folder, HOWEVER no dependency is packaged yet.
You will need to manually copy from your maven cache the following dependencies (jars) to your "ext" folder to make the plugins work:
- Those directly concerning BIP in beam-backend/externals (*.jar)
- Their own dependencies :
  - org.mod4j.org.eclipse.emf:ecore:2.5.0
  - org.mod4j.org.eclipse.core:runtime:3.5.0
  - org.mod4j.org.eclipse:osgi:3.5.0
  - org.mod4j.org.eclipse.equinox:common:3.5.0
  - org.mod4j.org.eclipse.core:jobs:3.4.100
  - org.mod4j.org.eclipse.equinox:registry:3.4.100
  - org.mod4j.org.eclipse.equinox:preferences:3.2.300
  - org.mod4j.org.eclipse.core:contenttype:3.4.0
  - org.mod4j.org.eclipse.equinox:app:1.2.0
  - org.mod4j.org.eclipse.emf:common:2.5.0

All dependencies (Maven and manual BIP ones) should be configured to be packaged automatically thanks to the bin-release.xml file.

Other examples than helloworld-simple and helloworld-beam-fork have not been tested/fixed at all.
