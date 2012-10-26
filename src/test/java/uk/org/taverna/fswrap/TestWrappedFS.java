package uk.org.taverna.fswrap;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestWrappedFS {
	public static boolean DELETE = true;
	
	protected WrappedFileSystem fs;
	protected WrappedPath temp;
	protected Path origTemp;

	@Before
	public void wrapDefaultAndMakeTempFolder() throws IOException {
		fs = WrappedFileSystemProvider.wrapDefaultFs();
		origTemp = Files.createTempDirectory("fswrap-test");
		temp = fs.toWrappedPath(origTemp);
	}
	
	@After
	public void closeFileSystem() throws IOException {
		fs.close();
	}
	
	@After
	public void deleteTempFolder() throws IOException {
		if (DELETE) {
			Files.walkFileTree(origTemp, new DeletorVisitor());
		} else {
			System.out.println(origTemp);
		}
	}
	
	@Test
	public void sameOrig() throws Exception {
		assertSame(origTemp, temp.originalPath);
	}
	
	@Test
	public void createDirectory() throws Exception {
		String path = "folder";
		Path origTestFolder = origTemp.resolve(path);
		assertTrue(Files.notExists(origTestFolder));
		Path testFolder = temp.resolve(path);
		assertTrue(testFolder instanceof WrappedPath);
		assertTrue(Files.notExists(testFolder));
		
		testFolder = Files.createDirectory(temp.resolve(path));
		
		assertTrue(testFolder instanceof WrappedPath);
		assertTrue(Files.isDirectory(origTestFolder));
		assertTrue(Files.isDirectory(testFolder));
	}
	
	@Test
	public void newDirectoryStream() throws Exception {
		String path = "child";
		Path origTestFolder = origTemp.resolve(path);
		assertTrue(Files.notExists(origTestFolder));
		Path testFolder = temp.resolve(path);
		testFolder = Files.createDirectory(temp.resolve(path));

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(temp)) {
			int count = 0;
			for (Path p : stream) {
				assertTrue(p.equals(testFolder));
				count++;
			}
			assertEquals(1, count);
		}
 		
	}
	
	@Test
	public void createFile() throws Exception {
		String path = "file.txt";
		Path origFile = origTemp.resolve(path);
		assertTrue(Files.notExists(origFile));
		Path file = temp.resolve(path);
		assertTrue(file instanceof WrappedPath);
		assertTrue(Files.notExists(file));
		
		file = Files.createFile(temp.resolve(path));
		
		assertTrue(file instanceof WrappedPath);
		assertTrue(Files.isRegularFile(origFile));
		assertTrue(Files.isRegularFile(file));
		
		
	}
	
	@Test
	public void deleteFile() throws Exception {
		String path = "delete.txt";
		Path origFile = origTemp.resolve(path);
		Path file = temp.resolve(path);
		file = Files.createFile(temp.resolve(path));
		assertTrue(file instanceof WrappedPath);
		assertTrue(Files.isRegularFile(file));
		Files.delete(file);
		assertTrue(Files.notExists(file));
		assertTrue(Files.notExists(origFile));		
	}
	
	@Test
	public void readerWriter() throws Exception {
		// Note, this does implicitly test input/output stream and channels 
		
		String path = "input.txt";
		Path origFile = origTemp.resolve(path);
		Path file = temp.resolve(path);
		String lazyDog = "The lazy dog ate some food";
		
		try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.forName("utf8"))) {
			writer.append(lazyDog);
		}		
		try (BufferedReader reader = Files.newBufferedReader(file, Charset.forName("utf8"))) {
			String line = reader.readLine();
			assertEquals(lazyDog, line);
		}
		try (BufferedReader reader = Files.newBufferedReader(origFile, Charset.forName("utf8"))) {
			String line = reader.readLine();
			assertEquals(lazyDog, line);
		}
	}

	@Test
	public void copy() throws IOException {
		
		String fromPath = "from.txt";
		Path origFrom = origTemp.resolve(fromPath);
		Path from = temp.resolve(fromPath);
		assertTrue(Files.notExists(origFrom));

		from = Files.createFile(temp.resolve(fromPath));
		assertTrue(Files.isRegularFile(from));
		
		String toPath = "to.txt";
		Path origTo = origTemp.resolve(toPath);
		Path to = temp.resolve(toPath);
		assertTrue(Files.notExists(origTo));
		
		Files.copy(from, to);
		
		assertTrue(Files.isRegularFile(from));
		assertTrue(Files.isRegularFile(origFrom));
		assertTrue(Files.isRegularFile(to));
		assertTrue(Files.isRegularFile(origTo));

		
	}
	
	@Test
	public void move() throws IOException {
		String fromPath = "from.txt";
		Path origFrom = origTemp.resolve(fromPath);
		Path from = temp.resolve(fromPath);
		assertTrue(Files.notExists(origFrom));

		from = Files.createFile(temp.resolve(fromPath));
		assertTrue(Files.isRegularFile(from));
		
		String toPath = "to.txt";
		Path origTo = origTemp.resolve(toPath);
		Path to = temp.resolve(toPath);
		assertTrue(Files.notExists(origTo));
		
		Files.move(from, to);
		
		assertFalse(Files.isRegularFile(from));
		assertFalse(Files.isRegularFile(origFrom));
		assertTrue(Files.isRegularFile(to));
		assertTrue(Files.isRegularFile(origTo));

	}
	
	
}
