package it.s3sync.configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import it.s3sync.utils.GlobalPropertiesManager;

@Configuration
public class DbConfiguration {
	
	//FIXME: do transactions needed?
/*
	@Bean
	public TransactionTemplate transactionTemplate(MongoDatabaseFactory dbFactory) {
		return new TransactionTemplate(transactionManager(dbFactory));
	}

	@Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
*/
	@Bean
	@Primary
	public MongoOperations mongoOperations() throws UnsupportedEncodingException {
		return new MongoTemplate(mongoClient(), GlobalPropertiesManager.getProperty("mongo.db_name"));
	}

	@Bean
	@Primary
	public MongoClient mongoClient() throws UnsupportedEncodingException {
		return MongoClients.create("mongodb+srv://" + 
				URLEncoder.encode(GlobalPropertiesManager.getProperty("mongo.username"),"UTF-8") + ":" + 
				URLEncoder.encode(GlobalPropertiesManager.getProperty("mongo.password"),"UTF-8") + "@" +
				GlobalPropertiesManager.getProperty("mongo.connection_string"));
	}
}
