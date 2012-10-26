package uk.org.taverna.fswrap;

import static org.junit.Assert.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestListener extends TestWrappedFS {
	private final class LoggingListener implements
			FileSystemEventListener {
		List<String> log = new ArrayList<>();

		@Override
		public void copied(Path source, Path target, CopyOption[] options) {
			log.add("copied " + source);
		}

		@Override
		public void createdDirectory(Path dir, FileAttribute<?>[] attrs) {
			log.add("createdDirectory " + dir);
			
		}

		@Override
		public void deleted(Path path) {
			log.add("deleted " + path);
			
		}

		@Override
		public void moved(Path source, Path target, CopyOption[] options) {
			log.add("moved " + source  + " to " + target);
			
		}

		@Override
		public void newByteChannel(Path path,
				Set<? extends OpenOption> options,
				FileAttribute<?>[] attrs, SeekableByteChannel byteChannel) {
			log.add("newByteChannel " + path);
			
		}

		@Override
		public void newFileSystem(WrappedFileSystem fs, Map<String, ?> env) {
			log.add("newFileSystem " + fs);
			
		}

		@Override
		public void setAttribute(Path path, String attribute, Object value,
				LinkOption[] options) {
			log.add("setAttribute " + path);				
		}
	}

	private LoggingListener loggingListener;
	
	@Test
	public void newFileSystem() throws Exception {
		
	}
	
	@Before
	public void addListener() {
		loggingListener = new LoggingListener();
		fs.provider().addFileSystemEventListener(loggingListener);		
	}		
	
	@After
	public void removeListener() {
		fs.provider().removeFileSystemEventListener(loggingListener);
	}

	@Override
	@Test
	public void createDirectory() throws Exception {
		super.createDirectory();
		assertTrue(
				loggingListener.log.get(0).startsWith("createdDirectory"));
	}
	
	@Override
	@Test
	public void createFile() throws Exception {
		super.createFile();		
		assertTrue(
				loggingListener.log.get(0).startsWith("newByteChannel"));
	}
	
	@Override
	@Test
	public void newDirectoryStream() throws Exception {		
		super.newDirectoryStream();
		assertTrue(
				loggingListener.log.get(0).startsWith("createdDirectory"));	}
	
	@Override
	@Test
	public void deleteFile() throws Exception {
		super.deleteFile();
		assertTrue(
				loggingListener.log.get(0).startsWith("newByteChannel"));
		assertTrue(
				loggingListener.log.get(1).startsWith("deleted"));
	}
	
	@Override
	@Test
	public void readerWriter() throws Exception {
		super.readerWriter();
		assertTrue(
				loggingListener.log.get(0).startsWith("newByteChannel"));
		assertTrue(
				loggingListener.log.get(1).startsWith("newByteChannel"));

	}

	
	@After
	public void printLog() throws Exception {
		for (String log : loggingListener.log) {
			System.out.println(log);			
		}
		System.out.println("-");
	}
	
}
