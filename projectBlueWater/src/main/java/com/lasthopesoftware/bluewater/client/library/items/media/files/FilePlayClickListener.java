package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.content.Context;
import android.view.View;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

public class FilePlayClickListener extends AbstractMenuClickHandler {
	private final List<ServiceFile> serviceFiles;
	private final int position;
	
	public FilePlayClickListener(NotifyOnFlipViewAnimator parent, int position, List<ServiceFile> serviceFiles) {
        super(parent);

		this.position = position;
		this.serviceFiles = serviceFiles;
	}
	
	@Override
	public void onClick(View v) {
		final Context context = v.getContext();

		FileStringListUtilities
			.promiseSerializedFileStringList(serviceFiles)
			.then(new VoidResponse<>(fileStringList -> PlaybackService.launchMusicService(context, position, fileStringList)));

        super.onClick(v);
	}
}
