package uk.org.taverna.fswrap;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DeletorVisitor extends SimpleFileVisitor<Path> {
	// Implemented as in http://stackoverflow.com/a/8685959
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		Files.delete(file);
		return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
			throws IOException {
		Files.delete(file);
		return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
		if (exc == null) {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		} else {
			// directory iteration failed; propagate exception
			throw exc;
		}
	}
}