package com.namehillsoftware.client.browsing.library.access;

import com.annimon.stream.Stream;
import com.namehillsoftware.client.browsing.library.repository.Library;
import com.namehillsoftware.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public class FakeLibraryProvider implements ILibraryProvider {
	private final Library[] libraries;

	public FakeLibraryProvider(Library... libraries) {
		this.libraries = libraries;
	}

	@NotNull
	@Override
	public Promise<Library> getLibrary(@NotNull LibraryId libraryId) {
		return new Promise<>(Stream.of(libraries).filter(l -> l.getLibraryId().equals(libraryId)).findFirst().get());
	}

	@NotNull
	@Override
	public Promise<Collection<Library>> getAllLibraries() {
		return new Promise<>(Stream.of(libraries).toList());
	}
}
