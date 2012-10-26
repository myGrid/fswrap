package uk.org.taverna.fswrap;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Iterator;

public class UCFDirectoryStream implements DirectoryStream<Path> {

	private DirectoryStream<Path> originalDirectoryStream;
	private UCFFileSystem fileSystem;
	private UCFPath dir;

	public UCFDirectoryStream(Path dir, DirectoryStream<Path> originalDirectoryStream) {
		if (! (dir instanceof UCFPath)) {
			throw new ProviderMismatchException("Wrong Path type " + dir.getClass());
		}
		this.dir = (UCFPath) dir;
		this.originalDirectoryStream = originalDirectoryStream;
		this.fileSystem = (UCFFileSystem) dir.getFileSystem();
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
				return fileSystem.toUcfPath(origIt.next());
			}

			@Override
			public void remove() {
				origIt.remove();
			}
			
		};
	}

}
