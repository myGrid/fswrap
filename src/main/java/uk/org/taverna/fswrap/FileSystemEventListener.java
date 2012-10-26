package uk.org.taverna.fswrap;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

public interface FileSystemEventListener extends EventListener {

	void newFileSystem(WrappedFileSystem fs, Map<String, ?> env);

	void newByteChannel(Path path, Set<? extends OpenOption> options,
			FileAttribute<?>[] attrs, SeekableByteChannel byteChannel);

	void createdDirectory(Path dir, FileAttribute<?>[] attrs);

	void deleted(Path path);

	void copied(Path source, Path target, CopyOption[] options);

	void moved(Path source, Path target, CopyOption[] options);

	void setAttribute(Path path, String attribute, Object value,
			LinkOption[] options);

}
