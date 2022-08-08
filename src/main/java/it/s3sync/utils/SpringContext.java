package it.s3sync.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {

	private static ApplicationContext applicationContextReference;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		applicationContextReference = applicationContext;
		
	}
	
	public static <T> T getBean(Class<T> t) {
		return applicationContextReference.getBean(t);
	}

}
