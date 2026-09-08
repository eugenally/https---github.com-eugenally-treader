package com.edu.bootstring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*동일한 출처가 아닌경우 요청을 허용하기 위한 설정파일.
컨테이너 시작시 자동으로 빈이 생성되넝 적용된다. */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer{
  //addMapping() : 설정된 경로에 대하 CORS를 허용한다. 만약 특정경로만 허용하고 싶다면  "/apis/*"와 같이 작성하면 된다.
  //allowedOriginPatterns (): 특정 오리진에 대해 허용한다. 이 경우 "http://sample.com"과 같이 작성한다.
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "PUT", "DELETE")
      .allowedHeaders("Authorization", "Content-Type")
      .exposedHeaders("Custom-Header")
      .allowCredentials(true)
      .maxAge(3600);
  }

}
