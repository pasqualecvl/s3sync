package it.s3sync.configuration;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.s3sync.listener.AmqpSyncListener;
import it.s3sync.utils.GlobalPropertiesManager;
import it.s3sync.utils.UserSpecificPropertiesManager;

@Configuration
public class MessageBrokerConfiguration {

	@Bean
	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
				GlobalPropertiesManager.getProperty("amqp.url"));
		connectionFactory.setUsername(GlobalPropertiesManager.getProperty("amqp.user"));
		connectionFactory.setPassword(GlobalPropertiesManager.getProperty("amqp.password"));
		connectionFactory.setVirtualHost(GlobalPropertiesManager.getProperty("amqp.vhost"));
		return connectionFactory;
	}

	@Bean
	public Queue queue() {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("x-message-ttl", 3000);
		return new Queue(GlobalPropertiesManager.getProperty("amqp.queue") + "_" +
				UserSpecificPropertiesManager.getConfiguration().getAlias(), false, false, false, args);
	}

	@Bean
	public FanoutExchange fanoutExchange() {
		return new FanoutExchange(GlobalPropertiesManager.getProperty("amqp.notification_topic"));
	}

	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue()).to(fanoutExchange());
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(GlobalPropertiesManager.getProperty("amqp.queue") + "_"
				+ UserSpecificPropertiesManager.getConfiguration().getAlias());
		container.setMessageListener(listenerAdapter);
		return container;
	}

	@Bean
	public MessageListenerAdapter messageListenerAdapter(AmqpSyncListener amqpSyncListener) {
		MessageListenerAdapter listener = new MessageListenerAdapter(amqpSyncListener, "receiveSyncMessage");
		listener.setMessageConverter(jackson2JsonMessageConverter());
		return listener;

	}

	@Bean
	public AmqpTemplate amqpTemplate() {
		RabbitTemplate template = new RabbitTemplate(connectionFactory());
		template.setExchange(GlobalPropertiesManager.getProperty("amqp.notification_topic"));
		template.setMessageConverter(jackson2JsonMessageConverter());
		return template;
	}
	
	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
		jackson2JsonMessageConverter.setAlwaysConvertToInferredType(true);
		DefaultClassMapper defaultClassMapper = new DefaultClassMapper();
		defaultClassMapper.setTrustedPackages("it.s3sync.sync");
		jackson2JsonMessageConverter.setClassMapper(defaultClassMapper);
		return jackson2JsonMessageConverter;
	}


}
