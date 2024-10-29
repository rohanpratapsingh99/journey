package com.adobe.aem.guides.wknd.core.models;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = ConfigRetriever.class, immediate = true)
@Designate(ocd = ConfigRetrieverImpl.Configs.class)
public class ConfigRetrieverImpl implements ConfigRetriever {

	@Override
	public String getLoginPage() {
		return loginPath;
	}

	@Override
	public String getSignUpPage() {
		return signUpPath;
	}

	@Override
	public String getAdminConsolePage() {
		return adminConsolePath;
	}

	@Override
	public String getHomePage() {
		return homepagePath;
	}

	@ObjectClassDefinition(name = "WKND General Configs", description = "This OSGI configuration provides various settings to configure the integration.")
	public @interface Configs {
		@AttributeDefinition(name = "Login path", type = AttributeType.STRING)
		public String getLoginPage() default "/content/wknd/us/en/about-us/login";

		@AttributeDefinition(name = "Signup Page", type = AttributeType.STRING)
		public String getSignUpPage() default "/content/wknd/us/en/about-us/signup";

		@AttributeDefinition(name = "Admin Console Page",  type = AttributeType.STRING)
		public String getAdminConsolePage() default "/content/wknd/us/en/about-us/admin-console";

		@AttributeDefinition(name = "Hoempage", type = AttributeType.STRING)
		public String getHomePage() default "/content/wknd/us/en";

	}

	private String loginPath;
	private String signUpPath;
	private String adminConsolePath;
	private String homepagePath;

	@Activate
	protected void activate(Configs config) {
		loginPath = config.getLoginPage();
		signUpPath = config.getSignUpPage();
		adminConsolePath = config.getAdminConsolePage();
		homepagePath = config.getHomePage();
	}

	
}