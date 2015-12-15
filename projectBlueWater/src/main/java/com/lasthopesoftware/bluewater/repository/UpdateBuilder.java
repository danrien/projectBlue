package com.lasthopesoftware.bluewater.repository;

import java.util.ArrayList;

/**
 * Created by david on 12/14/15.
 */
public class UpdateBuilder {
	private final StringBuilder sqlStringBuilder;
	private final ArrayList<String> setters = new ArrayList<>();
	private String filter;

	public UpdateBuilder(String tableName) {
		sqlStringBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
	}

	public UpdateBuilder addSetter(String columnName) {
		setters.add(columnName);
		return this;
	}

	public UpdateBuilder setFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public String buildQuery() {
		for (String setter : setters)
			sqlStringBuilder.append(setter).append(" = :").append(setter).append(", ");

		sqlStringBuilder.delete(sqlStringBuilder.length() - 3, sqlStringBuilder.length() - 1);

		if (filter != null && !filter.isEmpty())
			sqlStringBuilder.append(" WHERE ").append(filter);

		return sqlStringBuilder.toString();
	}
}
