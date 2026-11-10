package com.barofit.report.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ReportJacksonConfig {

  @Bean
  public ObjectMapper reportObjectMapper() {
    return new ObjectMapper();
  }
}
