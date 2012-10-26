package uk.org.taverna.fswrap;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class WrappedFileSystemProvider extends FileSystemProvider {

	public static final String SCHEME = "wrap";

	/**
	 * A FileSystemAlreadyExistsException which keeps a reference to the
	 * existing FileSystem.
	 * <p>
	 * This is useful because the {@link WrappedFileSystemProvider} keeps the
	 * existing file systems as weak references; this exception (thrown by
	 * {@link WrappedFileSystemProvider#newFileSystem(URI, Map)}) would ensure
	 * that the file system is not garbage collected until the handling of the
	 * exception has been finished.
	 * 
	 */
	public static final class FileSystemAlreadyExistsExceptionWithRef extends
			FileSystemAlreadyExistsException {
		private final FileSystem fs;

		private FileSystemAlreadyExistsExceptionWithRef(String msg,
				FileSystem fs) {
			super(msg);
			this.fs = fs;
		}

		public FileSystem getExistingFileSystem() {
			return fs;
		}
	}

	public static class Listeners implements FileSystemEventListener,
			Iterable<FileSystemEventListener> {

		private Set<FileSystemEventListener> registered = new LinkedHashSet<FileSystemEventListener>();

		public synchronized void add(FileSystemEventListener l) {
			registered.add(l);
		}

		public synchronized List<FileSystemEventListener> all() {
			// Make a thread-safe snapshot
			return new ArrayList<FileSystemEventListener>(registered);
		}

		@Override
		public void copied(Path source, Path target, CopyOption[] options) {
			for (FileSystemEventListener l : this) {
				l.copied(source, target, options);
			}
		}

		@Override
		public void createdDirectory(Path dir, FileAttribute<?>[] attrs) {
			for (FileSystemEventListener l : this) {
				l.createdDirectory(dir, attrs);
			}
		}

		@Override
		public void deleted(Path path) {
			for (FileSystemEventListener l : this) {
				l.deleted(path);
			}
		}

		@Override
		public Iterator<FileSystemEventListener> iterator() {
			// iterate over a copy
			return all().iterator();
		}

		@Override
		public void moved(Path source, Path target, CopyOption[] options) {
			for (FileSystemEventListener l : this) {
				l.moved(source, target, options);
			}
		}

		@Override
		public void newByteChannel(Path path,
				Set<? extends OpenOption> options, FileAttribute<?>[] attrs,
				SeekableByteChannel byteChannel) {
			for (FileSystemEventListener l : this) {
				l.newByteChannel(path, options, attrs, byteChannel);
			}
		}

		@Override
		public void newFileSystem(WrappedFileSystem fs, Map<String, ?> env) {
			for (FileSystemEventListener l : this) {
				l.newFileSystem(fs, env);
			}
		}

		public synchronized void remove(FileSystemEventListener l) {
			registered.remove(l);
		}

		@Override
		public void setAttribute(Path path, String attribute, Object value,
				LinkOption[] options) {
			for (FileSystemEventListener l : this) {
				l.setAttribute(path, attribute, value, options);
			}
		}

	}

	private Map<URI, WeakReference<WrappedFileSystem>> cache = Collections
			.synchronizedMap(new HashMap<URI, WeakReference<WrappedFileSystem>>());
	private Listeners listeners = new Listeners();

	private Map<FileSystem, WrappedFileSystem> origToWrappedFs = Collections.synchronizedMap(
			new WeakHashMap<FileSystem, WrappedFileSystem>());

	public void addFileSystemEventListener(FileSystemEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		getOriginalProvider(path).checkAccess(toOriginalPath(path), modes);
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options)
			throws IOException {
		getOriginalProvider(source).copy(toOriginalPath(source),
				toOriginalPath(target), options);
		listeners.copied(source, target, options);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs)
			throws IOException {
		getOriginalProvider(dir).createDirectory(toOriginalPath(dir), attrs);
		listeners.createdDirectory(dir, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		getOriginalProvider(path).delete(toOriginalPath(path));
		listeners.deleted(path);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
			Class<V> type, LinkOption... options) {
		return getOriginalProvider(path).getFileAttributeView(
				toOriginalPath(path), type, options);
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return new WrappedFileStore(getOriginalProvider(path).getFileStore(
				toOriginalPath(path)));
	}

	@Override
	public WrappedFileSystem getFileSystem(URI uri) {
		WeakReference<WrappedFileSystem> ref = cache.get(uri);
		if (ref == null) {
			throw new FileSystemNotFoundException();
		}
		WrappedFileSystem fs = ref.get();
		if (fs == null) {
			cache.remove(uri);
			throw new FileSystemNotFoundException();
		}
		return fs;
	}

	public List<FileSystemEventListener> getFileSystemEventListeners() {
		return listeners.all();
	}

	protected WrappedFileSystem getFileSystemWrapping(
			FileSystem originalFileSystem) {
		return origToWrappedFs.get(originalFileSystem);
	}

	protected FileSystemProvider getOriginalProvider(Path path) {
		Path p = toOriginalPath(path);
		return getOriginalProvider(p.toUri().getScheme());
	}

	protected FileSystemProvider getOriginalProvider(String originalScheme) {
		for (FileSystemProvider provider : FileSystemProvider
				.installedProviders()) {
			if (provider.getScheme().equals(originalScheme)) {
				return provider;
			}
		}
		throw new IllegalArgumentException("No provider for scheme "
				+ originalScheme);
	}

	@Override
	public WrappedPath getPath(URI uri) {
		Path originalPath = getOriginalProvider(uri.getScheme()).getPath(
				toOrigUri(uri));
		WrappedFileSystem fs = getFileSystemWrapping(originalPath
				.getFileSystem());
		return fs.toWrappedPath(originalPath);
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return getOriginalProvider(path).isHidden(toOriginalPath(path));
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return getOriginalProvider(path).isSameFile(toOriginalPath(path),
				toOriginalPath(path2));
	}

	@Override
	public void move(Path source, Path target, CopyOption... options)
			throws IOException {
		getOriginalProvider(source).move(toOriginalPath(source),
				toOriginalPath(target), options);
		listeners.moved(source, target, options);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		SeekableByteChannel byteChannel = getOriginalProvider(path)
				.newByteChannel(toOriginalPath(path), options, attrs);
		listeners.newByteChannel(path, options, attrs, byteChannel);
		return byteChannel;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir,
			Filter<? super Path> filter) throws IOException {
		return new WrappedDirectoryStream(dir, getOriginalProvider(dir)
				.newDirectoryStream(toOriginalPath(dir), filter));
	}

	@Override
	public WrappedFileSystem newFileSystem(URI uri, Map<String, ?> env)
			throws IOException {
		FileSystem originalFs;
		boolean closeOriginal = false;
		try {
			originalFs = FileSystems.getFileSystem(toOrigUri(uri));
		} catch (FileSystemNotFoundException ex) {
			originalFs = FileSystems.newFileSystem(toOrigUri(uri), env);
			closeOriginal = true;
		}
		if (cache.containsKey(uri)) {
			final WrappedFileSystem fs = cache.get(uri).get();
			if (fs != null) {
			}
			throw new FileSystemAlreadyExistsExceptionWithRef(
					"File system exists for " + uri, fs);
		}
		WrappedFileSystem fs = new WrappedFileSystem(this, uri, originalFs, closeOriginal);
		cache.put(uri, new WeakReference<WrappedFileSystem>(fs));
		origToWrappedFs.put(originalFs, fs);
		listeners.newFileSystem(fs, env);
		return fs;
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path,
			Class<A> type, LinkOption... options) throws IOException {
		return getOriginalProvider(path).readAttributes(toOriginalPath(path),
				type, options);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes,
			LinkOption... options) throws IOException {
		return getOriginalProvider(path).readAttributes(toOriginalPath(path),
				attributes, options);
	}

	public void removeFileSystemEventListener(FileSystemEventListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value,
			LinkOption... options) throws IOException {
		setAttribute(toOriginalPath(path), attribute, value, options);
		listeners.setAttribute(path, attribute, value, options);
	}

	protected Path toOriginalPath(Path other) {
		if (other == null) {
			return null;
		}
		if (other instanceof WrappedPath) {
			WrappedPath wrappedPath = (WrappedPath) other;
			return wrappedPath.originalPath;
		} else {
			throw new ProviderMismatchException("Wrong Path type "
					+ other.getClass());
		}
	}

	protected URI toOrigUri(URI uri) {
		return URI.create(uri.getSchemeSpecificPart());
	}

	protected URI toWrappedUri(URI uri) {
		try {
			// TODO: Check that #fragment is preserved
			return new URI(SCHEME, uri.toASCIIString(), null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Could not transform URI " + uri);
		}
	}

	public static WrappedFileSystem wrapDefaultFs() {
		URI uri;
		try {
			uri = new URI(WrappedFileSystemProvider.SCHEME, "file:///", null);
		} catch (URISyntaxException e1) {
			throw new RuntimeException("Can't make URI", e1);
		}
		Map<String, ?> env = Collections.emptyMap();
		try {
			return (WrappedFileSystem) FileSystems.newFileSystem(uri, env);
		} catch (FileSystemAlreadyExistsExceptionWithRef e) {
			return (WrappedFileSystem) e.getExistingFileSystem();
		} catch (IOException e) {
			throw new RuntimeException("Can't find default file system for "
					+ uri);
		}
	}

	public void closeFilesystem(WrappedFileSystem wrappedFileSystem) {
		cache.remove(wrappedFileSystem.getUri());
		origToWrappedFs.remove(wrappedFileSystem.getOriginalFilesystem());
	}

}
