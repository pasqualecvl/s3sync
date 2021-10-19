package it.s3sync.web.dto.request;

import java.util.List;

public class DeleteTrashItemsRequest {

	private List<String> keys;

	public List<String> getKeys() {
		return keys;
	}
	
	public void setKeys(List<String> keys) {
		this.keys = keys;
	}
}
