package uk.org.taverna.fswrap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class UCFFileStore extends FileStore {

	private FileStore zipFileStore;

	public UCFFileStore(FileStore zipFileStore) {
		this.zipFileStore = zipFileStore;
	}
	
	public String name() {
		return zipFileStore.name();
	}

	public int hashCode() {
		return zipFileStore.hashCode();
	}

	public String type() {
		return "ucf";
	}

	public boolean isReadOnly() {
		return zipFileStore.isReadOnly();
	}

	public long getTotalSpace() throws IOException {
		return zipFileStore.getTotalSpace();
	}

	public long getUsableSpace() throws IOException {
		return zipFileStore.getUsableSpace();
	}

	public long getUnallocatedSpace() throws IOException {
		return zipFileStore.getUnallocatedSpace();
	}

	public boolean supportsFileAttributeView(
			Class<? extends FileAttributeView> type) {
		return zipFileStore.supportsFileAttributeView(type);
	}

	public boolean supportsFileAttributeView(String name) {
		return zipFileStore.supportsFileAttributeView(name);
	}

	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(
			Class<V> type) {
		return zipFileStore.getFileStoreAttributeView(type);
	}

	public Object getAttribute(String attribute) throws IOException {
		return zipFileStore.getAttribute(attribute);
	}


}