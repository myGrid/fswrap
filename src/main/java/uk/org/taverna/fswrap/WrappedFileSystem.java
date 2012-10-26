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

public class WrappedFileSystem extends FileSystem {

	private final FileSystem originalFilesystem;
	private final WrappedFileSystemProvider provider;
	private WrappedFileStore wrappedFileStore;
	private URI uri;
	private boolean closeOriginalOnClose;

	public WrappedFileSystem(WrappedFileSystemProvider provider, URI uri,
			FileSystem originalFs, boolean closeOriginalOnClose) {
		this.provider = provider;
		this.originalFilesystem = originalFs;
		this.uri = uri;
		this.closeOriginalOnClose = closeOriginalOnClose;
	}

	@Override
	public void close() throws IOException {
		if (closeOriginalOnClose) {
			originalFilesystem.close();
		}
		provider().closeFilesystem(this);	
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		if (wrappedFileStore == null) {
			wrappedFileStore = new WrappedFileStore(getOriginalFilesystem()
					.getFileStores().iterator().next());
		}
		return Collections.singleton((FileStore) wrappedFileStore);
	}

	public FileSystem getOriginalFilesystem() {
		return originalFilesystem;
	}

	@Override
	public Path getPath(String first, String... more) {
		Path origPath = getOriginalFilesystem().getPath(first, more);
		return toWrappedPath(origPath);
	}

	@Override
	public PathMatcher getPathMatcher(final String syntaxAndPattern) {
		return new PathMatcher() {
			PathMatcher zipPathMatcher = getOriginalFilesystem()
					.getPathMatcher(syntaxAndPattern);

			@Override
			public boolean matches(Path path) {
				return zipPathMatcher.matches(provider.toOriginalPath(path));
			}
		};
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return toWrappedPaths(getOriginalFilesystem().getRootDirectories());
	}

	@Override
	public String getSeparator() {
		return getOriginalFilesystem().getSeparator();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		return getOriginalFilesystem().getUserPrincipalLookupService();
	}

	@Override
	public boolean isOpen() {
		return getOriginalFilesystem().isOpen();
	}

	@Override
	public boolean isReadOnly() {
		return getOriginalFilesystem().isReadOnly();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		return getOriginalFilesystem().newWatchService();
	}

	@Override
	public WrappedFileSystemProvider provider() {
		return provider;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return getOriginalFilesystem().supportedFileAttributeViews();
	}

	protected WrappedPath toWrappedPath(Path origPath) {
		if (origPath == null) {
			return null;
		}
		return new WrappedPath(this, origPath);
	}

	protected Iterable<Path> toWrappedPaths(Iterable<Path> origPaths) {
		List<Path> wrapped = new ArrayList<Path>();
		for (Path orig : origPaths) {
			// where's my yield !!??
			wrapped.add(toWrappedPath(orig));
		}
		return wrapped;
	}

	public URI getUri() {
		return uri;
	}

}
