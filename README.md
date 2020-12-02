# GeoEvent Delay Processor

This custom processor provides the capability to delay execution of one or more events based on a configurable 
delay interval and unit. Incoming GeoEvents are added to a queue and held in memory until their intervals have 
elapsed, after which they are processed asynchronously in the GeoEvent service.

This type of processor can be useful where administrators want to support streaming location data to users but 
do not want to support near real-time updates.  Or when your data is delivered in a batch of records and you need to enforce temporal order.

![Example](geoevent-delay-processor.png?raw=true)

<p> Examples:
<p><b>Time Field = RECEIVED_TIME, Allow Duplicates = Yes, Use TRACK_ID = No</b><br>These settings will simply delay every event by the delay time.
<p><b>Time Field = TIME_START, Allow Duplicates = No, Use TRACK_ID = No</b><br>These settings will use the TIME_START value to determine if an event has already been received with the same start time value. If an event has an identical value for TIME_START to a previous event, the current event will be dropped.
<p><b>Time Field = TIME_START, Allow Duplicates = No, Use TRACK_ID = Yes</b><br>These settings will use the TRACK_ID and the TIME_START values to determine if an event has already been received with those same values. If an event has identical values for both TRACK_ID and TIME_START to a previous event, the current event will be dropped.

## Features
* GeoEvent Delay Processor

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Server SDK are installed on your machine.
2. Run 'mvn install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Copy the *.jar files under the 'target' sub-folder(s) into the [ArcGIS-GeoEvent-Server-Install-Directory]/deploy folder.

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Support

This component is not officially supported as an Esri product. The source code is available under the Apache License. 

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Usage

* The following parameters are supported:
  * `Delay Value` Specifies a constant value by which GeoEvents will be delayed before further processing.
   Delayed events are added to queue and held in memory.
  * `Delay Value Unit` specifies the time unit for the delay value.
  * `Delay Time Field` Choose the field that the delay time will be added to (RECEIVE_TIME, TIME_START, or TIME_END).
  * `Allow Duplicates?` Should new events with the same timestamp as an event already in the queue be allowed to be added to the queue? If duplicates are allowed, multiple events with the same timestamp may enter the queue.
  * `Use TRACK_ID` Should each unique TRACK_ID get its own queue, or should all events be stored in a single queue.


