### Location logger for StartupBus

Simple GPS location collector and forwarder service and UI.

#### Develop

    ant debug
    adb install -r bin/BusDroid-debug.apk

#### Architecture

Nutshell, still very flexible

  + GPS logger service, periodically save location info into local database
  + Forwarder service, periodically send saved data to remote server when connection is available

### External code

#### GPSLogger

Using GPS logger service based on [GPSLogger], which is under Apache License,
available for modification and distribution, and no "notice.txt" provided.

#### SimpleGeo

The [SimpleGeo] interaction layer.

    git clone https://github.com/simplegeo/java-simplegeo.git
    cd java-simplegeo
    ant storage-jar-without-libs

At the moment I have to use "without-libs", otherwise end up with compilation
errors. Should solve this. In the meantime two external libraries need
to be included: signpost-core and signpost-commonshttp4. Use them from the
java-simplegeo dir, or from the [Signpost download page][Signpost]



[GPSLogger]: http://code.google.com/p/gpslogger/ "GPSLogger on Google Code"
[SimpleGeo]: http://simplegeo.com/ "SimpleGeo website"
[Signpost]: http://code.google.com/p/oauth-signpost/downloads/list "Signpost download on Google Code"
