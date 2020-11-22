package com.namehillsoftware.projectblue.shared;

import java.util.Objects;

public abstract class AbstractIntKeyStringValue implements IIntKeyStringValue {
	private int mKey;
	private String mValue;

	protected AbstractIntKeyStringValue(int key, String value) {
		this.setKey(key);
		this.setValue(value);
	}

	protected AbstractIntKeyStringValue() {
	}

	/**
	 * @return the mKey
	 */
	public int getKey() {
		return mKey;
	}

	/**
	 * @param mKey the mKey to set
	 */
	public void setKey(int mKey) {
		this.mKey = mKey;
	}

	/**
	 * @return the mValue
	 */
	public String getValue() {
		return mValue;
	}

	/**
	 * @param value the mValue to set
	 */
	public void setValue(String value) {
		this.mValue = value;
	}

	@Override
	public int compareTo(IIntKeyStringValue another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final AbstractIntKeyStringValue that = (AbstractIntKeyStringValue) o;
		return mKey == that.mKey && Objects.equals(mValue, that.mValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mKey, mValue);
	}
}
