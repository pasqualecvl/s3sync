package it.pasqualecavallo.s3sync.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.service.ManageFolderService;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

@RestController
public class ManageFolderController {

	@Autowired
	private ManageFolderService manageFolderService;
	
	@PostMapping("/api/folder/add")
	public void addFolder(@RequestBody AddFolderRequest addFolderRequest) {
		
	}
	
}
