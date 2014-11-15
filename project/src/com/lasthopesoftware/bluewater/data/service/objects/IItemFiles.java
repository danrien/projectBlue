package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;

public interface IItemFiles {
	ArrayList<File> getFiles();
	ArrayList<File> getFiles(int option);
	void getFileStringList(OnCompleteListener<String> onGetStringListComplete);
	void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete);
}
