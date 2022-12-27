package actors;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RestTemplateTest {

    private final Log log = LogFactory.getLog(getClass());

    private URI baseUri;

    private ConfigurableApplicationContext server;

    private RestTemplate restTemplate;

    private MovieRepository movieRepository;

    private URI moviesUri;

    @Before
    public void setUp() {
        server = new SpringApplicationBuilder()
                .properties(Collections.singletonMap("server.port", "0"))
                .sources(DemoApplication.class).run();

        int port = server.getEnvironment().getProperty("local.server.port",
                Integer.class, 8080);

        restTemplate = server.getBean(RestTemplate.class);
        baseUri = URI.create("http://localhost:" + port + "/");
        moviesUri = URI.create(baseUri + "movies");
        movieRepository = server.getBean(MovieRepository.class);
    }

    @After
    public void tearDown() {
        if (null != server) {
            server.close();
        }
    }

    @Test
    public void testRestTemplate() {
        // <1>
        ResponseEntity<Movie> postMovieResponseEntity =
                restTemplate.postForEntity(moviesUri, new Movie("Forest Gump"), Movie.class);
        URI uriOfNewMovie = postMovieResponseEntity.getHeaders().getLocation();
        log.info("the new movie lives at " + uriOfNewMovie);

        // <2>
        JsonNode mapForMovieRecord = restTemplate.getForObject(uriOfNewMovie, JsonNode.class);
        log.info("\t..read as a Map.class: " + mapForMovieRecord);
        assertEquals(mapForMovieRecord.get("title").asText(), postMovieResponseEntity.getBody().title);

        // <3>
        Movie movieReference = restTemplate.getForObject(uriOfNewMovie, Movie.class);
        assertEquals(movieReference.title, postMovieResponseEntity.getBody().title);
        log.info("\t..read as a Movie.class: " + movieReference);

        // <4>
        ResponseEntity<Movie> movieResponseEntity = restTemplate.getForEntity(uriOfNewMovie, Movie.class);
        assertEquals(movieResponseEntity.getStatusCode(), HttpStatus.OK);
        assertEquals(MediaType.parseMediaType("application/json"),
                movieResponseEntity.getHeaders().getContentType());
        log.info("\t..read as a ResponseEntity<Movie>: " + movieResponseEntity);

        // <5>
        ParameterizedTypeReference<CollectionModel<Movie>> movies =
                new ParameterizedTypeReference<CollectionModel<Movie>>() {
                };
        ResponseEntity<CollectionModel<Movie>> moviesResponseEntity = restTemplate
                .exchange(moviesUri, HttpMethod.GET, null, movies);
        CollectionModel<Movie> movieResources = moviesResponseEntity.getBody();
        movieResources.forEach(log::info);
        //assertEquals(movieResources.getContent().size(), movieRepository.count());//FIXME repository.count() is 0
        movieResources.getLinks().stream()
                .forEach(link -> System.out.printf("Link:[%s] rel:[%s]\n", link, link.getRel().value()));
        assertEquals(1, movieResources.getLinks().stream()
                        .peek(link -> System.out.println("Link:" + link))
                .filter(m -> "self".equals(m.getRel().value()))
                .count());
    }
}
