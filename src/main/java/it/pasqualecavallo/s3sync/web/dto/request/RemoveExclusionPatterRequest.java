package it.pasqualecavallo.s3sync.web.dto.request;

import javax.validation.constraints.NotEmpty;

import it.pasqualecavallo.s3sync.web.dto.request.validator.RegExp;

public class RemoveExclusionPatterRequest {

	@RegExp
	private String regExp;
	
	@NotEmpty
	private String remoteFolder;
	
	public String getRegExp() {
		return regExp;
	}
	
	public void setRegExp(String regExp) {
		this.regExp = regExp;
	}
	
	public String getRemoteFolder() {
		return remoteFolder;
	}
	
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}
}
