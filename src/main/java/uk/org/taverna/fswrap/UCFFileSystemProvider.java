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


public class UCFFileSystemProvider extends FileSystemProvider {

	private static final String CREATE = "create";
	private static final String MIMETYPE = "mimetype";
	private static final String JAR = "jar";
	private static final String UCF = "ucf";
	private Map<URI, WeakReference<UCFFileSystem>> cache = new HashMap<URI, WeakReference<UCFFileSystem>>();
	private FileSystemProvider zipProvider;
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
		public void newFileSystem(UCFFileSystem fs, Map<String, ?> env) {
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
		return UCF;
	}

	@Override
	public UCFFileSystem newFileSystem(URI uri, Map<String, ?> env)
			throws IOException {
		FileSystem zipFs = FileSystems.newFileSystem(toZipUri(uri), env);
		String mimeType = (String) env.get(MIMETYPE);
		if (mimeType == null && env.containsKey(CREATE)) {
			throw new IllegalArgumentException(String.format(
					"Missing required env '%s' with create", MIMETYPE));
		}
		UCFFileSystem fs = new UCFFileSystem(this, uri, mimeType, zipFs);
		cache.put(uri, new WeakReference<UCFFileSystem>(fs));
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
	
	protected URI toZipUri(URI uri) {
		return changeUriScheme(uri, JAR);
	}

	protected URI toUcfUri(URI uri) {
		return changeUriScheme(uri, UCF);
	}
	
	protected FileSystemProvider getZipProvider() {
		if (zipProvider == null) {
			for (FileSystemProvider provider : FileSystemProvider
					.installedProviders()) {
				if (provider.getScheme().equals(JAR)) {
					zipProvider = provider;
				}
			}
		}
		return zipProvider;
	}

	@Override
	public UCFFileSystem getFileSystem(URI uri) {
		WeakReference<UCFFileSystem> ref = cache.get(uri);
		if (ref == null) {
			throw new FileSystemNotFoundException();
		}
		UCFFileSystem fs = ref.get();
		if (fs == null) {
			cache.remove(uri);			
			throw new FileSystemNotFoundException();
		}
		return fs;
	}

	@Override
	public UCFPath getPath(URI uri) {
		Path zipPath = getZipProvider().getPath(toZipUri(uri));		
		URI base;
		try {
			base = new URI(UCF, uri.getSchemeSpecificPart().replaceAll("!.*", ""), null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URI " + uri);
		}
		return getFileSystem(base).toUcfPath(zipPath);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		SeekableByteChannel byteChannel = getZipProvider().newByteChannel(toZipPath(path), options, attrs);
		listeners.newByteChannel(path, options, attrs, byteChannel);
		return byteChannel;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir,
			Filter<? super Path> filter) throws IOException {
		return new UCFDirectoryStream(dir, getZipProvider().newDirectoryStream(toZipPath(dir), filter));
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs)
			throws IOException {
		getZipProvider().createDirectory(toZipPath(dir), attrs);
		listeners.createdDirectory(dir, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		getZipProvider().delete(toZipPath(path));
		listeners.deleted(path);
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options)
			throws IOException {
		getZipProvider().copy(toZipPath(source), toZipPath(target), options);		
		listeners.copied(source, target, options);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options)
			throws IOException {
		getZipProvider().move(toZipPath(source), toZipPath(target), options);
		listeners.moved(source, target, options);
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return getZipProvider().isSameFile(toZipPath(path), toZipPath(path2));
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return getZipProvider().isHidden(toZipPath(path));
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return new UCFFileStore(getZipProvider().getFileStore(toZipPath(path)));
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		zipProvider.checkAccess(toZipPath(path), modes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
			Class<V> type, LinkOption... options) {
		return getZipProvider().getFileAttributeView(toZipPath(path), type, options);
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path,
			Class<A> type, LinkOption... options) throws IOException {
		return getZipProvider().readAttributes(toZipPath(path), type, options);
	}
	
	protected Path toZipPath(Path other) {
		if (other == null) {
			return null;
		}
		if (other instanceof UCFPath) {
			UCFPath ucfPath = (UCFPath) other;
			return ucfPath.zipPath;
		} else {
			throw new ProviderMismatchException("Wrong Path type " + other.getClass());
		}
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes,
			LinkOption... options) throws IOException {
		return getZipProvider().readAttributes(toZipPath(path), attributes, options);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value,
			LinkOption... options) throws IOException {
		setAttribute(toZipPath(path), attribute, value, options);
		listeners.setAttribute(path, attribute, value, options);
	}

}
