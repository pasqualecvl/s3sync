package it.s3sync.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.s3sync.service.TrashService;
import it.s3sync.web.dto.request.DeleteTrashItemsRequest;
import it.s3sync.web.dto.response.DeleteTrashItemResponse;
import it.s3sync.web.dto.response.Folder;

@RestController
public class TrashController {

	@Autowired
	private TrashService trashService;
	
	@GetMapping(value="/api/trash/navigate")
	public Folder navigateTrash(@RequestParam String relativePath) {
		return trashService.navigate(relativePath);
	}
	
	@DeleteMapping(value="/api/trash/delete")
	public DeleteTrashItemResponse deleteTrashedFile(@RequestBody DeleteTrashItemsRequest request) {
		return trashService.deleteAll(request.getKeys());
	}
	
//	@PostMapping(value="/api/trash/recover")
//	public RecoverTrashedFileResponse recoverTrashedFile(@RequestBody RecoverTrashedFileRequest request) {
//		return trashService.recoverAll(request.getToRemoteFolder(), request.getKeys());
//	}
	
}
