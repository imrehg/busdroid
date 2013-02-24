### Location logger for StartupBus

Simple GPS location collector and forwarder service and UI.

#### Develop

    ant debug
    adb install -r bin/BusDroid-debug.apk

Currently it is targeting API level 16 (Android 4.1 Jelly Bean), with minimum level 10 (given the phones I have to test, HTC Butterfly, and HTC Desire on CyanogenMod 7).

#### Architecture

Nutshell, still very flexible

  + GPS logger service, periodically save location info into local database
  + Forwarder service, periodically send saved data to remote server when connection is available

#### Usage

 + use "Get Configuration" with the URL provided by the StartupBus team
 + add choose your bus after the config update
 + add oAuth token given by the team
 + enable GPS
 + start logging and keep logging

### External code

#### GPSLogger

Using GPS logger service based on [GPSLogger], which is under Apache License,
available for modification and distribution, and no "notice.txt" provided.

[GPSLogger]: http://code.google.com/p/gpslogger/ "GPSLogger on Google Code"
