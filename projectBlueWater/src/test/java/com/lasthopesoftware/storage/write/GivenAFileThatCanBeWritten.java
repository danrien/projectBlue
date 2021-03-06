package com.lasthopesoftware.storage.write;

import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GivenAFileThatCanBeWritten {

	public static class WhenCheckingForPermissions {

		private boolean fileWriteIsPossible;

		@Before
		public void before() {
			final FileWritePossibleArbitrator fileWritePossibleArbitrator = new FileWritePossibleArbitrator();
			final File file = mock(File.class);
			when(file.exists()).thenReturn(true);
			when(file.canWrite()).thenReturn(true);

			fileWriteIsPossible = fileWritePossibleArbitrator.isFileWritePossible(file);
		}

		@Test
		public void thenFileWriteIsPossible() {
			assertThat(this.fileWriteIsPossible).isTrue();
		}
	}
}
