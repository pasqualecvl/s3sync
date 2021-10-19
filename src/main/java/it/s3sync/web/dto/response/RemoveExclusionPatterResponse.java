package it.s3sync.web.dto.response;

import java.util.List;

public class RemoveExclusionPatterResponse extends RestBaseResponse {

	private List<String> exclusionPatterns;

	public List<String> getExclusionPatterns() {
		return exclusionPatterns;
	}

	public void setExclusionPatterns(List<String> exclusionPatterns) {
		this.exclusionPatterns = exclusionPatterns;
	}

}
