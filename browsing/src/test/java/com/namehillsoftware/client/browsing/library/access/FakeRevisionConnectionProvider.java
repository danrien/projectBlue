package com.namehillsoftware.client.browsing.library.access;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;


public class FakeRevisionConnectionProvider extends FakeConnectionProvider {

	private int syncRevision;

	public FakeRevisionConnectionProvider() {
		mapResponse(
			(params) ->
				new FakeConnectionResponseTuple(
					200,
					("<Response Status=\"OK\">" +
						"<Item Name=\"Master\">1192</Item>" +
						"<Item Name=\"Sync\">" + syncRevision + "</Item>" +
						"<Item Name=\"LibraryStartup\">1501430846</Item>" +
					"</Response>").getBytes()),
			"Library/GetRevision");
	}

	public void setSyncRevision(int syncRevision) {
		this.syncRevision = syncRevision;
	}
}
