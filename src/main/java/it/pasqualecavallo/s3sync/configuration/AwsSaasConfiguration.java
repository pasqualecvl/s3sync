package it.pasqualecavallo.s3sync.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;



@Configuration
public class AwsSaasConfiguration {

	
	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.region(Region.of(GlobalPropertiesManager.getProperty("s3.region")))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										GlobalPropertiesManager.getProperty("s3.access_key"),
										GlobalPropertiesManager.getProperty("s3.access_secret"))
				)).build();
	}
	
	/*
	@Bean
	public DynamoDbClient dynamoClient() {
		return DynamoDbClient.builder().
				region(Region.of(PropertiesManager.getProperty("dynamo.region")))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										PropertiesManager.getProperty("dynamo.access_key"),
										PropertiesManager.getProperty("dynamo.access_secret"))
				)).build();
				
	}
	
	@Bean
	public SqsClient sqsClient() {
		return SqsClient.builder()
			.region(Region.of(GlobalPropertiesManager.getProperty("sqs.region")))
			.credentialsProvider(
					StaticCredentialsProvider.create(
							AwsBasicCredentials.create(
									GlobalPropertiesManager.getProperty("sqs.access_key"),
									GlobalPropertiesManager.getProperty("sqs.access_secret"))
							)
					)
			.build();
	}
	
	*/

}
