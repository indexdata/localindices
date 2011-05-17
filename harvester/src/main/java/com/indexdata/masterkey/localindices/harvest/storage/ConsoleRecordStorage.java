package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class ConsoleRecordStorage implements RecordStorage {

	private boolean overrideMode;

	@Override
	public void begin() throws IOException {
		System.out.println("Begin");
	}

	@Override
	public void commit() throws IOException {
		System.out.println("Commit");
	}

	@Override
	public void rollback() throws IOException {
		System.out.println("Rollback");
	}

	@Override
	public void purge() throws IOException {
		System.out.println("Purge");
	}

	@Override
	public void setOverwriteMode(boolean mode) {
		System.out.println("setOverrideMode");
		overrideMode = mode;
	}

	@Override
	public boolean getOverwriteMode() {
		return overrideMode;
	}

	@Override
	public OutputStream getOutputStream() {
		throw new RuntimeException("OutputStream interface not supported");
	}

	@Override
	public void databaseStart(Map<String, String> properties) {
		System.out.println("databaseStart");
	}

	@Override
	public void databaseEnd() {
		System.out.println("databaseEnd");
	}

	@Override
	public void add(Map<String, Collection<Serializable>> keyValues) {
		System.out.println("adding Map...");
	}

	@Override
	public void add(Record record) {
		System.out.print("Add Record{id=" + record.getId() + ", ");
		Map<String, Collection<Serializable>> map = record.getValues();
		System.out.println(map);
		System.out.println("}");
}

	@Override
	public Record get(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String id) {
		System.out.println("Delete Record{id=" + id + "}");
	}
}