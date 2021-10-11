package it.pasqualecavallo.s3sync.web.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.service.ManageFolderService;
import it.pasqualecavallo.s3sync.web.dto.request.AddFolderRequest;
import it.pasqualecavallo.s3sync.web.dto.response.AddFolderResponse;

@RestController
public class ManageFolderController {

	@Autowired
	private ManageFolderService manageFolderService;
	
	@PostMapping(value = "/api/folder/add")
	public AddFolderResponse addFolder(@RequestBody @Valid AddFolderRequest addFolderRequest) {
		return manageFolderService.addFolder(addFolderRequest.getLocalFolder(), addFolderRequest.getRemoteFolder());
	}
	
}
