package it.pasqualecavallo.s3sync.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.pasqualecavallo.s3sync.listener.AmqpSyncListener;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;

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
		return new Queue(GlobalPropertiesManager.getProperty("amqp.queue"), false);
	}

	@Bean
	public TopicExchange topicExchange() {
		return new TopicExchange(GlobalPropertiesManager.getProperty("amqp.notification_topic"));
	}

	@Bean
	public Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(GlobalPropertiesManager.getProperty("amqp.binding_key"));
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(GlobalPropertiesManager.getProperty("amqp.queue"));
		container.setMessageListener(listenerAdapter);
		return container;
	}
	
	@Bean
	public MessageListenerAdapter messageListenerAdapter(AmqpSyncListener amqpSyncListener) {
		return new MessageListenerAdapter(amqpSyncListener, "receiveSyncMessage");
	}

}
