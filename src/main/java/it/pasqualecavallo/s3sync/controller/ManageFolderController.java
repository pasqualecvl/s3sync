package it.pasqualecavallo.s3sync.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.controller.dto.AddFolderRequest;
import it.pasqualecavallo.s3sync.service.ManageFolderService;

@RestController
public class ManageFolderController {

	@Autowired
	private ManageFolderService manageFolderService;
	
	@PostMapping(value = "/api/folder/add")
	public void addFolder(@RequestBody @Valid AddFolderRequest addFolderRequest) {
		manageFolderService.addFolder(addFolderRequest.getLocalFolder(), addFolderRequest.getRemoteFolder());
	}
	
}
