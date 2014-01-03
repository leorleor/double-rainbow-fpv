double-rainbow-fpv
==================

Double Rainbow FPV

Hi, thanks for taking a look.  This is primordial code for a dual symmetric 4G FPV control/video/data link.  The RX and TX both run on Android devices, the RX uses USB OTG to connect to an Arduino that outputs ppm to control a normal RC model.  A goal is an affordable, simple, and expandable long range digital FPV link.

This is not a proper app yet, not even a beta.  Figured I would share the code to help anyone jump start similar projects (I definitely appreciate all the examples I found out there while getting this working!).

Here is a brief ground-based demo:
http://youtu.be/94o_GtKqFVs

Generally the hardware performed pretty well.  I look forward to getting this into a normal usable state as time allows.


Prereqs
==================

This requires usb-serial-for-android which can also be found in git hub.  The android project.properties file will likely need to be adjusted to point at the relative location to which you clone usb-serial-for-android.


Notes
==================

Sorry, the code in here is beyond messy.  Especially sloppy threading which occasionally causes crashes on connection.  This is because I've so far been laser focused on putting the pieces together end-to-end and proving they can work.  I wanted to get an idea of things like how feasible a low latency video link on commodity hardware was, and how phone orientation sensors would handle vibrations.  I did get around to a few cool extras like camera zoom, external sensors, and OSD working along the way.  

The camera api is not well documented and different devices had their own quirks, or do not support the hard coded resolutions, and crash.  I have been using a Motorola Razr M, and sometimes a Galaxy Nexus.

I plan to switch to firmata to control arduino from android.  It seems to have good community support and does everything my app does now.  I may still need to use the library version and custom code to handle ppm inputs.  Kudos to Antoine for his android apps that largely convinced me.


Peace
==================

Peace out, leorleor.
