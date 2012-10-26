package uk.org.taverna.fswrap;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Iterator;

public class WrappedDirectoryStream implements DirectoryStream<Path> {

	private WrappedFileSystem fileSystem;
	private DirectoryStream<Path> originalDirectoryStream;

	public WrappedDirectoryStream(Path dir,
			DirectoryStream<Path> originalDirectoryStream) {
		if (!(dir instanceof WrappedPath)) {
			throw new ProviderMismatchException("Wrong Path type "
					+ dir.getClass());
		}
		this.originalDirectoryStream = originalDirectoryStream;
		this.fileSystem = (WrappedFileSystem) dir.getFileSystem();
	}

	@Override
	public void close() throws IOException {
		originalDirectoryStream.close();
	}

	@Override
	public Iterator<Path> iterator() {
		final Iterator<Path> origIt = originalDirectoryStream.iterator();
		return new Iterator<Path>() {
			@Override
			public boolean hasNext() {
				return origIt.hasNext();
			}

			@Override
			public Path next() {
				return fileSystem.toWrappedPath(origIt.next());
			}

			@Override
			public void remove() {
				origIt.remove();
			}

		};
	}

}
