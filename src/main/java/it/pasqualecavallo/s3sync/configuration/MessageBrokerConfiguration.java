package it.pasqualecavallo.s3sync.configuration;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.pasqualecavallo.s3sync.listener.AmqpSyncListener;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

@Configuration
public class MessageBrokerConfiguration {

	@Bean
	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(GlobalPropertiesManager.getProperty("amqp.url"));
		connectionFactory.setUsername(GlobalPropertiesManager.getProperty("amqp.user"));
		connectionFactory.setPassword(GlobalPropertiesManager.getProperty("amqp.password"));
		connectionFactory.setVirtualHost(GlobalPropertiesManager.getProperty("amqp.vhost"));
		return connectionFactory;
	}

	@Bean
	public Queue queue() {
		return new Queue(GlobalPropertiesManager.getProperty("amqp.queue") + "_" +
				UserSpecificPropertiesManager.getProperty("client.alias"), false, false, false, null);
	}

	@Bean
	public FanoutExchange fanoutExchange() {
		return new FanoutExchange(GlobalPropertiesManager.getProperty("amqp.notification_topic"));
	}

	@Bean
	public Binding binding() {
		return BindingBuilder
				.bind(queue())
				.to(fanoutExchange());
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(GlobalPropertiesManager.getProperty("amqp.queue") + "_" +
				UserSpecificPropertiesManager.getProperty("client.alias"));
		container.setMessageListener(listenerAdapter);
		return container;
	}
	
	@Bean
	public MessageListenerAdapter messageListenerAdapter(AmqpSyncListener amqpSyncListener) {
		return new MessageListenerAdapter(amqpSyncListener, "receiveSyncMessage");
	}
	
	@Bean
	public AmqpTemplate amqpTemplate() {
		RabbitTemplate template = new RabbitTemplate(connectionFactory());
		template.setExchange(GlobalPropertiesManager.getProperty("amqp.notification_topic"));
		return template;
	}

}
