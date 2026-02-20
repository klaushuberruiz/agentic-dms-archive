package com.dms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureSearchConfig {

    @Bean
    @ConfigurationProperties(prefix = "azure.ai.search")
    public AzureSearchProperties azureSearchProperties() {
        return new AzureSearchProperties();
    }

    public static class AzureSearchProperties {
        private String endpoint;
        private String apiKey;
        private String indexName;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }
    }
}
