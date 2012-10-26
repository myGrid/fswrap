fswrap
======

(c) 2012 University of Manchester, UK <support@mygrid.org.uk>

Licensed under the [GNU Lesser General Public License (LGPL) 2.1][6]. 
See LICENSE.txt for the full terms of LGPL 2.1.

This is a FileSystemProvider[1] for the Java NIO.2 API [1] that allows wrapping
calls to an existing file system, for instance for the purpose of logging,
tracing paths in a manifest, etc.


Requisites
----------

* Java 1.7 or newer
* Maven 3.0 or newer (for building)


Building
--------

* `mvn clean install`


Usage
-----

The uk.org.taverna.fswrap.WrappedFileSystemProvider should automatically 
be registered under the URI scheme "wrap".

For instance, to wrap the default file system: 

    URI uri = URI.create("wrap:file:///");
    try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
        // ...
    }

Or simply:
	try (WrappedFileSystem fs = WrappedFileSystemProvider.wrapDefaultFs()) {
	    // ...
	}
	
	

[1]: http://docs.oracle.com/javase/tutorial/essential/io/fileio.html
[2]: http://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/filesystemprovider.html