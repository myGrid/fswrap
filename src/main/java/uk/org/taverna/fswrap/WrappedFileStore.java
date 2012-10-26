package uk.org.taverna.fswrap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class WrappedFileStore extends FileStore {

	private FileStore originalFileStore;

	public WrappedFileStore(FileStore zipFileStore) {
		this.originalFileStore = zipFileStore;
	}
	
	public Object getAttribute(String attribute) throws IOException {
		return originalFileStore.getAttribute(attribute);
	}

	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(
			Class<V> type) {
		return originalFileStore.getFileStoreAttributeView(type);
	}

	public long getTotalSpace() throws IOException {
		return originalFileStore.getTotalSpace();
	}

	public long getUnallocatedSpace() throws IOException {
		return originalFileStore.getUnallocatedSpace();
	}

	public long getUsableSpace() throws IOException {
		return originalFileStore.getUsableSpace();
	}

	public int hashCode() {
		return originalFileStore.hashCode();
	}

	public boolean isReadOnly() {
		return originalFileStore.isReadOnly();
	}

	public String name() {
		return originalFileStore.name();
	}

	public boolean supportsFileAttributeView(
			Class<? extends FileAttributeView> type) {
		return originalFileStore.supportsFileAttributeView(type);
	}

	public boolean supportsFileAttributeView(String name) {
		return originalFileStore.supportsFileAttributeView(name);
	}

	public String type() {
		return "wrap";
	}


}