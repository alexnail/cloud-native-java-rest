package actors;

import java.net.URI;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TraversonConfiguration {

 private int port;

 private URI baseUri;

 //@formatter:off
 @EventListener
 public void embeddedPortAvailable(ServletWebServerInitializedEvent e) {
  this.port = e.getWebServer().getPort();
  this.baseUri = URI.create("http://localhost:" + this.port + '/');
 }
 //@formatter:on

 // <1>
 @Bean
 @Lazy
 Traverson traverson(RestTemplate restTemplate) {
  Traverson traverson = new Traverson(this.baseUri, MediaTypes.HAL_JSON);
  traverson.setRestOperations(restTemplate);
  return traverson;
 }
}
