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
	
	public String name() {
		return originalFileStore.name();
	}

	public int hashCode() {
		return originalFileStore.hashCode();
	}

	public String type() {
		return "wrap";
	}

	public boolean isReadOnly() {
		return originalFileStore.isReadOnly();
	}

	public long getTotalSpace() throws IOException {
		return originalFileStore.getTotalSpace();
	}

	public long getUsableSpace() throws IOException {
		return originalFileStore.getUsableSpace();
	}

	public long getUnallocatedSpace() throws IOException {
		return originalFileStore.getUnallocatedSpace();
	}

	public boolean supportsFileAttributeView(
			Class<? extends FileAttributeView> type) {
		return originalFileStore.supportsFileAttributeView(type);
	}

	public boolean supportsFileAttributeView(String name) {
		return originalFileStore.supportsFileAttributeView(name);
	}

	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(
			Class<V> type) {
		return originalFileStore.getFileStoreAttributeView(type);
	}

	public Object getAttribute(String attribute) throws IOException {
		return originalFileStore.getAttribute(attribute);
	}


}