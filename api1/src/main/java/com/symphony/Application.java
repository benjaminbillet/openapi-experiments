package com.symphony;

import io.swagger.configuration.SwaggerDocumentationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ComponentScan(basePackageClasses = { Application.class, SwaggerDocumentationConfig.class })
@SpringBootApplication
@EnableConfigurationProperties
@RequiredArgsConstructor
@Slf4j
public class Application {

  private final Environment env;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @PostConstruct
  public void initApplication() {
    // deal with a key store for https
    String protocol = "http";
    if (env.getProperty("server.ssl.key-store") != null) {
      protocol = "https";
    }

    String appName = env.getProperty("spring.application.name", "<unamed>");

    String serverPort = env.getProperty("server.port", "8080");
    String serverHost = resolveServerHost();

    String contextPath = env.getProperty("server.servlet.context-path");
    if (StringUtils.isBlank(contextPath)) {
      contextPath = "/";
    }

    String loggingBackend = LoggerFactory.getILoggerFactory().getClass().getName();

    logAppInfo(appName, protocol, serverHost, serverPort, contextPath, loggingBackend);
  }

  private String resolveServerHost() {
    String host = "localhost";
    try {
      host = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      log.warn("The host name could not be determined, use localhost");
    }
    return host;
  }

  private void logAppInfo(String appName, String protocol, String hostName, String serverPort, String contextPath, String loggingBackend) {
    log.info(
      "\n======================================================\n" +
        "Application '{}' is running\n" +
        "- local access:     {}://localhost:{}{}\n" +
        "- external access:  {}://{}:{}{}\n" +
        "- locale:           {}\n" +
        "- timezone:         {}\n" +
        "- profiles:         {}\n" +
        "- slf4j backend:    {}\n" +
        "======================================================",
      appName,
      protocol,
      serverPort,
      contextPath,
      protocol,
      hostName,
      serverPort,
      contextPath,
      Locale.getDefault(),
      ZoneId.systemDefault(),
      env.getActiveProfiles(),
      loggingBackend);
  }
}
