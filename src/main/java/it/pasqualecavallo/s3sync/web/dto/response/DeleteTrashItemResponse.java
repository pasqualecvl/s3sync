package it.pasqualecavallo.s3sync.web.dto.response;

import java.util.List;

public class DeleteTrashItemResponse extends RestBaseResponse {

	private List<String> deleted;
	
	public List<String> getDeleted() {
		return deleted;
	}
	
	public void setDeleted(List<String> deleted) {
		this.deleted = deleted;
	}
	
}
