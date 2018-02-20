JennyNet
========

Java client/server networking layer based on object serialisation and multi-threading.
Status: Alpha Results

Project Wiki [on Sourceforge](http://sourceforge.net/p/jennynet/wiki/Home/)

[User Manual](http://sourceforge.net/p/jennynet/wiki/User%20Manual/)

[Discussion](http://sourceforge.net/p/jennynet/discussion/)

-----------------

20.02.2018  Release 0.2.0 Alpha with improved functions; completed implementation
            and advanced testing of object and file transmissions.

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

21.10.2017  Release 0.1.0 Alpha version with all functions implemented; some
            may be error prone; some testing has been done. Can be used for
            proto-typing and testing.


