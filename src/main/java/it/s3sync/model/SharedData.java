package it.s3sync.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class SharedData {

	@Id
	private BigInteger id;
	
	private List<String> remoteFolders = new ArrayList<>();
	
	public BigInteger getId() {
		return id;
	}
	
	public List<String> getRemoteFolders() {
		return remoteFolders;
	}
	
	public void setRemoteFolders(List<String> remoteFolders) {
		this.remoteFolders = remoteFolders;
	}
	
}
