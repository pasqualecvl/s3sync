package it.s3sync.web.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.s3sync.service.ManageFolderService;
import it.s3sync.web.controller.advice.exception.BadRequestException;
import it.s3sync.web.dto.request.AddExclusionPatternRequest;
import it.s3sync.web.dto.request.AddFolderRequest;
import it.s3sync.web.dto.request.RemoveExclusionPatterRequest;
import it.s3sync.web.dto.request.RemoveFolderRequest;
import it.s3sync.web.dto.response.AddExclusionPatterResponse;
import it.s3sync.web.dto.response.AddFolderResponse;
import it.s3sync.web.dto.response.ListRemoteFolderResponse;
import it.s3sync.web.dto.response.ListSyncFoldersResponse;
import it.s3sync.web.dto.response.RemoveExclusionPatterResponse;
import it.s3sync.web.dto.response.RemoveFolderResponse;
import it.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;

@RestController
public class ManageFolderController {

	@Autowired
	private ManageFolderService manageFolderService;

	@GetMapping(value = "/api/folder/list_remote")
	public ListRemoteFolderResponse listRemoteFolder(@RequestParam Integer page, @RequestParam Integer pageSize) {
		return manageFolderService.listRemoteFolders(page, pageSize);
	}

	@PostMapping(value = "/api/folder/add")
	public AddFolderResponse addFolder(@RequestBody @Valid AddFolderRequest addFolderRequest) {
		if("Trash".equals(addFolderRequest.getRemoteFolder())) {
			throw new BadRequestException(ErrorMessage.E400_RESERVED_KEYWORK, "Trash");
		}
		return manageFolderService.addFolder(addFolderRequest.getLocalFolder(), addFolderRequest.getRemoteFolder());
	}

	@GetMapping(value = "/api/folder/list")
	public ListSyncFoldersResponse listSyncFolders(@RequestParam Integer page, @RequestParam Integer pageSize) {
		return manageFolderService.listFolders(page, pageSize);
	}

	@DeleteMapping(value = "/api/folder/remove")
	public RemoveFolderResponse removeFolder(@RequestBody @Valid RemoveFolderRequest removeFolderRequest) {
		if("Trash".equals(removeFolderRequest.getRemoteFolder())) {
			throw new BadRequestException(ErrorMessage.E400_RESERVED_KEYWORK, "Trash");
		}
		return manageFolderService.removeFolder(removeFolderRequest.getRemoteFolder());
	}

	@PostMapping(value = "/api/folder/add/exclusion_pattern")
	public AddExclusionPatterResponse addExclusionPattern(@RequestBody @Valid AddExclusionPatternRequest addExclusionPatter) {
		if("Trash".equals(addExclusionPatter.getRemoteFolder())) {
			throw new BadRequestException(ErrorMessage.E400_RESERVED_KEYWORK, "Trash");
		}
		return manageFolderService.addExclusionPattern(addExclusionPatter.getRegexp(), addExclusionPatter.getRemoteFolder());
	}

	@DeleteMapping(value = "/api/folder/remove/exclusion_pattern")
	public RemoveExclusionPatterResponse removeExclusionPattern(@RequestBody @Valid RemoveExclusionPatterRequest removeExclusionPatter) {
		return manageFolderService.removeExclusionPattern(removeExclusionPatter.getRegExp(), removeExclusionPatter.getRemoteFolder());
	}

}
