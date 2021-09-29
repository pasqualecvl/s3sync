package it.pasqualecavallo.s3sync.model;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Item {

	@Id
	private BigInteger _id;

	private String ownedByFolder;

	private String folderRelativePath;

	private String originalName;

	private String s3Name;

	private List<String> missingClientSynchronized;

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

	public String getFolderRelativePath() {
		return folderRelativePath;
	}

	public void setFolderRelativePath(String folderRelativePath) {
		this.folderRelativePath = folderRelativePath;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getS3Name() {
		return s3Name;
	}

	public void setS3Name(String s3Name) {
		this.s3Name = s3Name;
	}

	public List<String> getMissingClientSynchronized() {
		return missingClientSynchronized;
	}

	public void setMissingClientSynchronized(List<String> missingClientSynchronized) {
		this.missingClientSynchronized = missingClientSynchronized;
	}

}
