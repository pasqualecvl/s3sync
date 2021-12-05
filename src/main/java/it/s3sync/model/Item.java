package it.s3sync.model;

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
	
	private Boolean deleted = false;
	
	private String checksum;
	
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
	
	public Boolean getDeleted() {
		return deleted;
	}
	
	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
	
	public Item get() {
		return this;
	}
	
	public String getChecksum() {
		return checksum;
	}
	
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	@Override
	public String toString() {
		return "Item [_id=" + _id + ", ownedByFolder=" + ownedByFolder + ", originalName=" + originalName
				+ ", lastUpdate=" + lastUpdate + ", uploadedBy=" + uploadedBy + "]";
	}
	
	
}
