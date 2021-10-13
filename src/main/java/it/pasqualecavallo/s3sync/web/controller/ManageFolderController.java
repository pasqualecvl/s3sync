package it.pasqualecavallo.s3sync.web.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.service.ManageFolderService;
import it.pasqualecavallo.s3sync.web.dto.request.AddExclusionPatterRequest;
import it.pasqualecavallo.s3sync.web.dto.request.AddFolderRequest;
import it.pasqualecavallo.s3sync.web.dto.request.RemoveExclusionPatterRequest;
import it.pasqualecavallo.s3sync.web.dto.request.RemoveFolderRequest;
import it.pasqualecavallo.s3sync.web.dto.response.AddExclusionPatterResponse;
import it.pasqualecavallo.s3sync.web.dto.response.AddFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.ListSyncFoldersResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RemoveExclusionPatterResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RemoveFolderResponse;

@RestController
public class ManageFolderController {

	@Autowired
	private ManageFolderService manageFolderService;
	
	@PostMapping(value = "/api/folder/add")
	public AddFolderResponse addFolder(@RequestBody @Valid AddFolderRequest addFolderRequest) {
		return manageFolderService.addFolder(addFolderRequest.getLocalFolder(), addFolderRequest.getRemoteFolder());
	}
	
	@GetMapping(value = "/api/folder/list")
	public ListSyncFoldersResponse listSyncFolders(@RequestParam Integer page, @RequestParam Integer pageSize) {
		return manageFolderService.listFolders(page, pageSize);
	}
	
	@DeleteMapping(value = "/api/folder/remove")
	public RemoveFolderResponse removeFolder(@RequestBody @Valid RemoveFolderRequest removeFolderRequest) {
		return manageFolderService.removeFolder(removeFolderRequest.getRemoteFolder());
	}

	@PostMapping(value = "/api/folder/add/exclusion_pattern")
	public AddExclusionPatterResponse addExclusionPattern(@RequestBody @Valid AddExclusionPatterRequest addExclusionPatter) {
		return manageFolderService.addExclusionPattern(addExclusionPatter.getRegexp(), addExclusionPatter.getRemoteFolder());
	}

	@DeleteMapping(value = "/api/folder/remove/exclusion_pattern")
	public RemoveExclusionPatterResponse removeExclusionPattern(@RequestBody @Valid RemoveExclusionPatterRequest removeExclusionPatter) {
		return manageFolderService.removeExclusionPattern(removeExclusionPatter.getRegExp(), removeExclusionPatter.getRemoteFolder());
	}

}
