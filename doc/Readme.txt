Project JennyNet Network Layer
Package-Version 0.2.0

== Status

With version 0.2 object sending and file transfers as well as building
and closing of connections on client and server are expected to be fully
functional, where this functionality has been tested with JUnit tests. 
Error codes for closed connections and aborted file transfers have
been streamlined and documented in the project wiki.
The layer's current policy is that errors concerning the transmission of 
OBJECTS lead to the closing of affected connections. This is meaningful
as the correct sequence of objects may be critical to applications.

All other interface methods are expected to work correctly with the 
exception of methods and events concerning the IDLE state, which is not
yet implemented. Testing for these methods is not complete.

== External Software Packages

JennyNet involves external software which is readily supplied in the 
distribution package. Legal notices about these software is available at
the project wiki. It may not be assumed that the license agreement for
JennyNet stretches out over external software.


