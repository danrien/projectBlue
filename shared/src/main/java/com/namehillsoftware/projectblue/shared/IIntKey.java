package com.namehillsoftware.projectblue.shared;

public interface IIntKey<T> extends Comparable<T> {
	int getKey();
	void setKey(int key);
}
