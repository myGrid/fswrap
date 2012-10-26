package uk.org.taverna.fswrap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;


public class UCFPath implements Path {

	protected Path zipPath;
	private UCFFileSystem ucfFileSystem;

	public UCFFileSystem getFileSystem() {
		return ucfFileSystem;
	}

	public boolean isAbsolute() {
		return zipPath.isAbsolute();
	}

	public UCFPath getRoot() {
		return ucfFileSystem.toUcfPath(zipPath.getRoot());
	}

	public UCFPath getFileName() {
		return ucfFileSystem.toUcfPath(zipPath.getFileName());
	}

	public UCFPath getParent() {
		return ucfFileSystem.toUcfPath(zipPath.getParent());
	}

	public int getNameCount() {
		return zipPath.getNameCount();
	}

	public UCFPath getName(int index) {
		return ucfFileSystem.toUcfPath(zipPath.getName(index));
	}

	public UCFPath subpath(int beginIndex, int endIndex) {
		return ucfFileSystem.toUcfPath(zipPath.subpath(beginIndex, endIndex));
	}

	public boolean startsWith(Path other) {
		return zipPath.startsWith(ucfFileSystem.provider().toZipPath(other));
	}

	public boolean startsWith(String other) {
		return zipPath.startsWith(other);
	}

	public boolean endsWith(Path other) {
		return zipPath.endsWith(other);
	}

	public boolean endsWith(String other) {
		return zipPath.endsWith(other);
	}

	public UCFPath normalize() {
		return ucfFileSystem.toUcfPath(zipPath.normalize());
	}

	public UCFPath resolve(Path other) {
		return ucfFileSystem.toUcfPath(zipPath.resolve(ucfFileSystem.provider().toZipPath(other)));
	}

	public UCFPath resolve(String other) {
		return ucfFileSystem.toUcfPath(zipPath.resolve(other));
	}

	public UCFPath resolveSibling(Path other) {
		return ucfFileSystem.toUcfPath(zipPath.resolveSibling(other));
	}

	public UCFPath resolveSibling(String other) {
		return ucfFileSystem.toUcfPath(zipPath.resolveSibling(other));
	}

	public UCFPath relativize(Path other) {
		return ucfFileSystem.toUcfPath(zipPath.relativize(other));
	}

	public URI toUri() {
		return ucfFileSystem.provider().toUcfUri(zipPath.toUri());
	}

	public UCFPath toAbsolutePath() {
		return ucfFileSystem.toUcfPath(zipPath.toAbsolutePath());
	}

	public UCFPath toRealPath(LinkOption... options) throws IOException {
		return ucfFileSystem.toUcfPath(zipPath.toRealPath(options));
	}

	public File toFile() {
		return zipPath.toFile();
	}

	public WatchKey register(WatchService watcher, Kind<?>[] events,
			Modifier... modifiers) throws IOException {
		return zipPath.register(watcher, events, modifiers);
	}

	public WatchKey register(WatchService watcher, Kind<?>... events)
			throws IOException {
		return zipPath.register(watcher, events);
	}

	public Iterator<Path> iterator() {
		// TODO: Fix me
		return zipPath.iterator();
	}

	public int compareTo(Path other) {
		return zipPath.compareTo(ucfFileSystem.provider().toZipPath(other));
	}

	public boolean equals(Object other) {
		if (! (other instanceof Path)) {
			return false;
		}
		return zipPath.equals(ucfFileSystem.provider().toZipPath((Path) other));
	}

	
	public int hashCode() {
		return zipPath.hashCode();
	}

	public String toString() {
		return zipPath.toString();
	}
		
	protected UCFPath(UCFFileSystem ucfFileSystem, Path zipPath) {
		this.ucfFileSystem = ucfFileSystem;
		this.zipPath = zipPath;
	}



}
