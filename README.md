### Location logger for StartupBus

Simple GPS location collector and forwarder service and UI.

#### Develop

    ant debug
    adb install -r bin/Logger-debug.apk

#### Architecture

Nutshell, still very flexible

  + GPS logger service, periodically save location info into local database
  + Forwarder service, periodically send saved data to remote server when connection is available

### External code

Using GPS logger service based on [GPSLogger], which is under Apache License,
available for modification and distribution, and no "notice.txt" provided.

[GPSLogger]: http://code.google.com/p/gpslogger/ "GPSLogger on Google Code"
