package it.pasqualecavallo.s3sync.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import it.pasqualecavallo.s3sync.service.ManageConfigurationService;
import it.pasqualecavallo.s3sync.web.dto.response.ListGlobalConfigurationResponse;

@RestController
public class ManageConfigurationController {

	@Autowired
	private ManageConfigurationService manageConfigurationService;
	
	@GetMapping(value="/api/configuration/list_global")
	public ListGlobalConfigurationResponse listGlobalConfiguration() {
		//return manageConfigurationService.getGlobalConfiguration();
		return null;
	}
	
}
