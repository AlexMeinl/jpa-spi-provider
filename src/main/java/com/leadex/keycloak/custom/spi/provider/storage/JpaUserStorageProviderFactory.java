package com.leadex.keycloak.custom.spi.provider.storage;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class JpaUserStorageProviderFactory implements UserStorageProviderFactory<JpaUserStorageProvider> {

	private static final Logger log = LoggerFactory.getLogger(JpaUserStorageProviderFactory.class);
	public static final String PROVIDER_ID = "jpa-user-provider";

	protected List<ProviderConfigProperty> configMetadata;

	protected Properties properties = new Properties();

	@Override
	public void init(Config.Scope config) {
		InputStream is = getClass().getClassLoader().getResourceAsStream("/provider.properties");

		if (is == null) {
			log.warn("Could not find provider.properties in classpath");
		} else {
			try {
				properties.load(is);

				ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

				builder.property("config.key.jdbc.driver", "JDBC Driver Class",
						"JDBC Driver Class", ProviderConfigProperty.STRING_TYPE,
						properties.get("config.key.jdbc.driver"), null);

				builder.property("config.key.jdbc.url", "JDBC Url",
						"JDBC Url", ProviderConfigProperty.STRING_TYPE,
						properties.get("config.key.jdbc.url"), null);

				builder.property("config.key.db.username", "DB DB Username",
						"DB Username", ProviderConfigProperty.STRING_TYPE,
						properties.get("config.key.db.username"), null);

				builder.property("config.key.db.password", "DB password",
						"DB Password", ProviderConfigProperty.STRING_TYPE,
						properties.get("config.key.db.password"), null);

				configMetadata = builder.build();

			} catch (IOException ex) {
				log.error("Failed to load legacy.properties file", ex);
			}
		}
	}

	@Override
	public JpaUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		return new JpaUserStorageProvider(session, model);
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configMetadata;
	}

	@Override public List<ProviderConfigProperty> getConfigMetadata() {
		return getConfigProperties();
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		return "JPA Example User Storage Provider";
	}

	@Override
	public void close() {
		log.info("<<<<<< Closing factory");

	}

}
