package it.pasqualecavallo.s3sync.model;

import java.math.BigInteger;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Item {

	@Id
	private BigInteger _id;

	private String ownedByFolder;

	private String originalName;
	
	private Long lastUpdate;
	
	private String uploadedBy;

	public BigInteger get_id() {
		return _id;
	}

	public void set_id(BigInteger _id) {
		this._id = _id;
	}

	public String getOwnedByFolder() {
		return ownedByFolder;
	}

	public void setOwnedByFolder(String ownedByFolder) {
		this.ownedByFolder = ownedByFolder;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public Long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getUploadedBy() {
		return uploadedBy;
	}
	
	public void setUploadedBy(String uploadedBy) {
		this.uploadedBy = uploadedBy;
	}
	
	public Item get() {
		return this;
	}
}
