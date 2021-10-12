package it.pasqualecavallo.s3sync.web.dto.response;

import java.util.List;

public class ListSyncFoldersResponse extends RestBaseResponse {

	private List<SyncFolderResponse> list;
	
	public void setList(List<SyncFolderResponse> list) {
		this.list = list;
	}
	
	public List<SyncFolderResponse> getList() {
		return list;
	}
	
}