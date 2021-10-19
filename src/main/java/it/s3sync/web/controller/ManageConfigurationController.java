package it.s3sync.web.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.s3sync.model.AttachedClient;
import it.s3sync.model.AttachedClient.ClientConfiguration;
import it.s3sync.utils.UserSpecificPropertiesManager;
import it.s3sync.web.dto.UserConfigurationExchange;

@RestController
public class ManageConfigurationController {
	
	@GetMapping(value="/api/configuration/list")
	public UserConfigurationExchange listUserConfiguration() {
		return convertToRest(UserSpecificPropertiesManager.getConfiguration());
	}
	
	@PostMapping(value="/api/configuration/save")
	public void saveUserConfiguration(@RequestBody @Valid UserConfigurationExchange configuration) {
		convertToConfig(configuration);
	}

	private void convertToConfig(@Valid UserConfigurationExchange configuration) {
		AttachedClient client = UserSpecificPropertiesManager.getConfiguration();
		client.getClientConfiguration().setPreventFolderRecursiveRemoval(configuration.isPreventFolderRecursiveRemoval());
		client.getClientConfiguration().setRunSynchronizationOnStartup(configuration.isRunSynchronizationOnStartup());
		client.getClientConfiguration().setUseTrashOverDelete(configuration.isUseTrashOverDelete());
		UserSpecificPropertiesManager.setConfiguration(client);
	}

	private UserConfigurationExchange convertToRest(AttachedClient configuration) {
		ClientConfiguration clientConfiguration = configuration.getClientConfiguration();
		UserConfigurationExchange userConfigurationExchange = new UserConfigurationExchange();
		userConfigurationExchange.setPreventFolderRecursiveRemoval(clientConfiguration.isPreventFolderRecursiveRemoval());
		userConfigurationExchange.setRunSynchronizationOnStartup(clientConfiguration.isRunSynchronizationOnStartup());
		userConfigurationExchange.setUseTrashOverDelete(clientConfiguration.isUseTrashOverDelete());
		return userConfigurationExchange;
	}
	
	
}
