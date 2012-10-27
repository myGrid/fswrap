fswrap
======

(c) 2012 University of Manchester, UK <support@mygrid.org.uk>

Licensed under the [GNU Lesser General Public License (LGPL) 2.1][6]. 
See LICENSE.txt for the full terms of LGPL 2.1.

This is a [FileSystemProvider](http://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/filesystemprovider.html)
for the [Java NIO.2 API](http://docs.oracle.com/javase/tutorial/essential/io/fileio.html) 
that allows wrapping calls to an existing file system, for instance for the purpose 
of logging, tracing paths in a manifest, etc.


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
        Path p = fs.getPath("tmp", "file.txt");
        Files.createFile(p);
    }

Or simply:

	try (WrappedFileSystem fs = WrappedFileSystemProvider.wrapDefaultFs()) {
        Path p = fs.getPath("tmp", "file.txt");
        Files.createFile(p);
	}
	
To register to listen for events occurring through the WrappedFileSystem
(Note: not done directly on original file system):
     
     listener = new uk.org.taverna.fswrap.FileSystemEventListener() {
         @Override
         public void copied(Path source, Path target, CopyOption[] options) {
             System.out.println("copied " + source + " to " + target);
         }
         // ....
     };
     fs.provider().addFileSystemEventListener(listener);

You may want to use the uk.org.taverna.fswrap.FileSystemEventAdapter as a
superclass if you are not interested in every captured event. 

