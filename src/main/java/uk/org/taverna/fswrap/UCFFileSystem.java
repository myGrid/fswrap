package uk.org.taverna.fswrap;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UCFFileSystem extends FileSystem {

	private UCFFileSystemProvider provider;
	private FileSystem zipFs;
	private URI uri;
	private String mimeType;
	private UCFFileStore ucfFileStore;

	public UCFFileSystem(UCFFileSystemProvider provider, URI uri,
			String mimeType, FileSystem zipFs) {
		this.provider = provider;
		this.uri = uri;
		this.mimeType = mimeType;
		this.zipFs = zipFs;
	}

	@Override
	public UCFFileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {
		// TODO: Update manifest
		zipFs.close();
	}

	@Override
	public boolean isOpen() {
		return zipFs.isOpen();
	}

	@Override
	public boolean isReadOnly() {
		return zipFs.isReadOnly();
	}

	@Override
	public String getSeparator() {
		return zipFs.getSeparator();
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return toUcfPaths(zipFs.getRootDirectories());
	}

	protected Iterable<Path> toUcfPaths(Iterable<Path> zipPaths) {
		List<Path> ucfPaths = new ArrayList<Path>();
		for (Path zipPath : zipPaths) {
			// where's my yield !!??
			ucfPaths.add(toUcfPath(zipPath));
		}
		return ucfPaths;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		if (ucfFileStore == null) {
			ucfFileStore = new UCFFileStore(zipFs.getFileStores().iterator().next());
		}
		return Collections.singleton((FileStore)ucfFileStore);
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return zipFs.supportedFileAttributeViews();
	}

	@Override
	public Path getPath(String first, String... more) {
		Path zipPath = zipFs.getPath(first, more);
		return toUcfPath(zipPath);
	}
	@Override
	public PathMatcher getPathMatcher(final String syntaxAndPattern) {
		return new PathMatcher(){
			PathMatcher zipPathMatcher = zipFs.getPathMatcher(syntaxAndPattern);
			@Override
			public boolean matches(Path path) {
				return zipPathMatcher.matches(provider.toZipPath(path));
			}
		};
	}
	
	protected UCFPath toUcfPath(Path zipPath) {
		if (zipPath == null) {
			return null;
		}
		return new UCFPath(this, zipPath);
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		return zipFs.getUserPrincipalLookupService();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		return zipFs.newWatchService();
	}


}
