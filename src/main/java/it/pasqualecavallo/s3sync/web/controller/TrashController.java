package it.pasqualecavallo.s3sync.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.service.TrashService;
import it.pasqualecavallo.s3sync.web.dto.response.Folder;

@RestController
public class TrashController {

	@Autowired
	private TrashService trashService;
	
	@GetMapping(value="/api/trash/navigate")
	public Folder navigateTrash(@RequestParam String relativePath) {
		return trashService.navigate(relativePath);
	}
	
//	@DeleteMapping(value="/api/trash/delete")
//	public DeleteTrashItemResponse deleteTrashedFile() {
//		
//	}
//	
//	@PostMapping(value="/api/trash/recover")
//	public MoveTrashedFileResponse moveTrashedFile() {
//		
//	}
	
}
