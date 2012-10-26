package uk.org.taverna.fswrap;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

public class FileSystemEventAdapter implements FileSystemEventListener {
	@Override
	public void copied(Path source, Path target, CopyOption[] options) {
	}

	@Override
	public void createdDirectory(Path dir, FileAttribute<?>[] attrs) {
	}

	@Override
	public void deleted(Path path) {
	}

	@Override
	public void moved(Path source, Path target, CopyOption[] options) {
	}

	@Override
	public void newByteChannel(Path path, Set<? extends OpenOption> options,
			FileAttribute<?>[] attrs, SeekableByteChannel byteChannel) {
	}

	@Override
	public void newFileSystem(WrappedFileSystem fs, Map<String, ?> env) {
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value,
			LinkOption[] options) {
	}
}