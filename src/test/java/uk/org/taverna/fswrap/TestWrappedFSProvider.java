package uk.org.taverna.fswrap;

import static org.junit.Assert.*;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class TestWrappedFSProvider {
	@Test
	public void isDiscovered() throws Exception {

		for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
			if (fsp instanceof WrappedFileSystemProvider) {
				 return;
			}
//			System.out.println(fsp);
//			System.out.println(fsp.getScheme());
		}
		fail("Could not find file system provider "
				+ WrappedFileSystemProvider.class);
	}

	@Test
	public void constructor() throws Exception {
		Map<String, ?> env = Collections.emptyMap();
		URI uri = URI.create("wrap:file:/");
		WrappedFileSystem fs = new WrappedFileSystemProvider().newFileSystem(
				uri, env);
		assertNotNull(fs);
	}

	@Test
	public void newFileSystem() throws Exception {
 		Map<String, ?> env = Collections.emptyMap();
		URI uri = URI.create("wrap:file:/");
		FileSystem fs = FileSystems.newFileSystem(uri, env);
		assertTrue(fs instanceof WrappedFileSystem);
	}

	
	@Test
	public void toWrappedUri() throws Exception {
		WrappedFileSystemProvider fsp = new WrappedFileSystemProvider();
		URI origUri = URI.create("file://test");
		URI wrappedUri = fsp.toWrappedUri(origUri);
		assertEquals("wrap", wrappedUri.getScheme());
		assertEquals("file://test", wrappedUri.getRawSchemeSpecificPart());
		assertEquals(origUri, fsp.toOrigUri(wrappedUri));
	}
	
	@Test
	public void toWrappedUriEvil() throws Exception {
		WrappedFileSystemProvider fsp = new WrappedFileSystemProvider();
		URI origUri = URI.create("http://fred:soup@example.com/very/long/path%20with%20spaces/#andAHash%20with%20spaces");
		URI wrappedUri = fsp.toWrappedUri(origUri);
		assertEquals(null, wrappedUri.getFragment());		
		// Double escaped now
		assertTrue(wrappedUri.toASCIIString().contains("%2520with%2520spaces"));		
		URI restored = fsp.toOrigUri(wrappedUri);
		assertFalse(restored.toASCIIString().contains("%2520with%2520spaces"));
		assertEquals(origUri, restored);
	}
	
}
