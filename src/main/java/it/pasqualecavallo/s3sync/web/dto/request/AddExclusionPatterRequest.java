package it.pasqualecavallo.s3sync.web.dto.request;

import javax.validation.constraints.NotEmpty;

import it.pasqualecavallo.s3sync.web.dto.request.validator.RegExp;

public class AddExclusionPatterRequest {

	@NotEmpty
	private String remoteFolder;
	
	@RegExp
	private String regexp;

	public String getRemoteFolder() {
		return remoteFolder;
	}

	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}

	public String getRegexp() {
		return regexp;
	}

	public void setRegexp(String regexp) {
		this.regexp = regexp;
	}

}
