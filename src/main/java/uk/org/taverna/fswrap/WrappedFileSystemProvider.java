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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WrappedFileSystemProvider extends FileSystemProvider {

	private static final String JAR = "jar";
	private static final String WRAP = "wrap";
	private Map<URI, WeakReference<WrappedFileSystem>> cache = new HashMap<URI, WeakReference<WrappedFileSystem>>();
	private FileSystemProvider originalProvider;
	private Listeners listeners = new Listeners();

	public static class Listeners implements FileSystemEventListener,
	Iterable<FileSystemEventListener> {
		
		Set<FileSystemEventListener> registered = new LinkedHashSet<FileSystemEventListener>();
		
		@Override
		public Iterator<FileSystemEventListener> iterator() {
			// iterate over a copy
			return all().iterator();
		}
		
		public synchronized void add(FileSystemEventListener l) {
			registered.add(l);
		}
		
		public synchronized void remove(FileSystemEventListener l) {
			registered.remove(l);
		}
		
		public synchronized List<FileSystemEventListener> all() {
			// Make a thread-safe snapshot
			return new ArrayList<FileSystemEventListener>(registered);
		}
		
		@Override
		public void newFileSystem(WrappedFileSystem fs, Map<String, ?> env) {
			for (FileSystemEventListener l : this) {
				l.newFileSystem(fs, env);
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
		public void copied(Path source, Path target, CopyOption[] options) {
			for (FileSystemEventListener l : this) {
				l.copied(source, target, options);
			}			
		}

		@Override
		public void moved(Path source, Path target, CopyOption[] options) {
			for (FileSystemEventListener l : this) {
				l.moved(source, target, options);
			}			
		}

		@Override
		public void setAttribute(Path path, String attribute, Object value,
				LinkOption[] options) {
			for (FileSystemEventListener l : this) {
				l.setAttribute(path, attribute, value, options);
			}
		}

	}
	
	public void addFileSystemEventListener(FileSystemEventListener listener) {
		listeners.registered.add(listener);
	}
	public void removeFileSystemEventListener(FileSystemEventListener listener) {
		listeners.registered.remove(listener);
	}
	
	public List<FileSystemEventListener> getFileSystemEventListeners() {
		return listeners.all();
	}
	
	@Override
	public String getScheme() {
		return WRAP;
	}

	@Override
	public WrappedFileSystem newFileSystem(URI uri, Map<String, ?> env)
			throws IOException {
		FileSystem zipFs = FileSystems.newFileSystem(toOrigUri(uri), env);
		WrappedFileSystem fs = new WrappedFileSystem(this, uri, zipFs);
		cache.put(uri, new WeakReference<WrappedFileSystem>(fs));
		listeners.newFileSystem(fs, env);
		return fs;
	}

	protected URI changeUriScheme(URI uri, String toScheme) {
		try {
			return new URI(toScheme, uri.getRawSchemeSpecificPart(), uri.getRawFragment());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Could not transform URI " + uri);
		}		
	}
	
	protected URI toOrigUri(URI uri) {
		return changeUriScheme(uri, JAR);
	}

	protected URI toWrappedUri(URI uri) {
		return changeUriScheme(uri, WRAP);
	}
	
	protected FileSystemProvider getOriginalProvider() {
		if (originalProvider == null) {
			for (FileSystemProvider provider : FileSystemProvider
					.installedProviders()) {
				if (provider.getScheme().equals(JAR)) {
					originalProvider = provider;
				}
			}
		}
		return originalProvider;
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

	@Override
	public WrappedPath getPath(URI uri) {
		Path originalPath = getOriginalProvider().getPath(toOrigUri(uri));		
		URI base;
		try {
			base = new URI(WRAP, uri.getSchemeSpecificPart().replaceAll("!.*", ""), null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URI " + uri);
		}
		return getFileSystem(base).toWrappedPath(originalPath);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		SeekableByteChannel byteChannel = getOriginalProvider().newByteChannel(toOriginalPath(path), options, attrs);
		listeners.newByteChannel(path, options, attrs, byteChannel);
		return byteChannel;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir,
			Filter<? super Path> filter) throws IOException {
		return new WrappedDirectoryStream(dir, getOriginalProvider().newDirectoryStream(toOriginalPath(dir), filter));
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs)
			throws IOException {
		getOriginalProvider().createDirectory(toOriginalPath(dir), attrs);
		listeners.createdDirectory(dir, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		getOriginalProvider().delete(toOriginalPath(path));
		listeners.deleted(path);
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options)
			throws IOException {
		getOriginalProvider().copy(toOriginalPath(source), toOriginalPath(target), options);		
		listeners.copied(source, target, options);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options)
			throws IOException {
		getOriginalProvider().move(toOriginalPath(source), toOriginalPath(target), options);
		listeners.moved(source, target, options);
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return getOriginalProvider().isSameFile(toOriginalPath(path), toOriginalPath(path2));
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return getOriginalProvider().isHidden(toOriginalPath(path));
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return new WrappedFileStore(getOriginalProvider().getFileStore(toOriginalPath(path)));
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		originalProvider.checkAccess(toOriginalPath(path), modes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
			Class<V> type, LinkOption... options) {
		return getOriginalProvider().getFileAttributeView(toOriginalPath(path), type, options);
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path,
			Class<A> type, LinkOption... options) throws IOException {
		return getOriginalProvider().readAttributes(toOriginalPath(path), type, options);
	}
	
	protected Path toOriginalPath(Path other) {
		if (other == null) {
			return null;
		}
		if (other instanceof WrappedPath) {
			WrappedPath wrappedPath = (WrappedPath) other;
			return wrappedPath.originalPath;
		} else {
			throw new ProviderMismatchException("Wrong Path type " + other.getClass());
		}
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes,
			LinkOption... options) throws IOException {
		return getOriginalProvider().readAttributes(toOriginalPath(path), attributes, options);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value,
			LinkOption... options) throws IOException {
		setAttribute(toOriginalPath(path), attribute, value, options);
		listeners.setAttribute(path, attribute, value, options);
	}

}
