package com.cxs.client.http.vo;

import java.io.File;

public class FileInput {

	private File file;
	private String name;
	public FileInput() {
		
	}
	public FileInput(String name, File file) {
		super();
		this.name = name;
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	public String getName() {
		return name;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	

}
