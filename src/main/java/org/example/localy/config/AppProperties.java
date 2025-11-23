package org.example.localy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private S3 s3 = new S3();

  @Getter @Setter
  public static class S3 {
    private String bucket;
    private String region;
    private String cloudfrontDomain;
    private long presignSeconds = 3600;
    private String rootPrefix = "images";
  }
}
