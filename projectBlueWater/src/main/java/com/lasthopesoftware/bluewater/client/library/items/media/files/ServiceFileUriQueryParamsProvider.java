package com.lasthopesoftware.bluewater.client.library.items.media.files;

import com.namehillsoftware.lazyj.Lazy;

public final class ServiceFileUriQueryParamsProvider implements IServiceFileUriQueryParamsProvider {

	private static final Lazy<ServiceFileUriQueryParamsProvider> instance = new Lazy<>(ServiceFileUriQueryParamsProvider::new);

	private ServiceFileUriQueryParamsProvider() {}

	@Override
	public String[] getServiceFileUriQueryParams(ServiceFile serviceFile) {
		return new String[]{
			"File/GetFile",
			"File=" + Integer.toString(serviceFile.getKey()),
			"Quality=medium",
			"Conversion=Android",
			"Playback=0"};
	}

	public static ServiceFileUriQueryParamsProvider getInstance() {
		return instance.getObject();
	}
}
