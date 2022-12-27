package demo;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.MOCK)
public class CustomerProfilePhotoRestControllerTest {

    private static final Log log = LogFactory.getLog(CustomerProfilePhotoRestControllerTest.class);

    private static final File tmpFile = new File(
            System.getProperty("java.io.tmpdir"), "images/" + System.currentTimeMillis());

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MockMvc mockMvc;

    private Customer bruceBanner, peterParker;

    private byte[] dogeBytes;

    private final String urlTemplate = "/customers/{id}/photo";

    private final MediaType vndErrorMediaType = MediaType.parseMediaType("application/vnd.error");

    @AfterClass
    public static void after() throws Throwable {
        if (tmpFile.exists()) {
            Files.walkFileTree(tmpFile.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Before
    public void before() throws Throwable {
        Resource dogeResource = new ClassPathResource("doge.jpg");
        dogeBytes = StreamUtils.copyToByteArray(dogeResource.getInputStream());
        Assert.assertTrue(dogeResource.contentLength() > 0);
        Assert.assertTrue(dogeResource.exists());

        bruceBanner = customerRepository.findById(1L).orElseGet(
                () -> customerRepository.save(new Customer("Bruce", "Banner")));

        peterParker = customerRepository.findById(2L).orElseGet(
                () -> customerRepository.save(new Customer("Peter", "Parker")));

    }

    @Test
    public void nonBlockingPhotoUploadWithExistingCustomer() throws Exception {
        MvcResult mvcResult = mockMvc.perform(multipart(urlTemplate, bruceBanner.getId()).file("file", dogeBytes))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        MvcResult mvcResultWithLocation = mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isCreated())
                .andExpect(header().string("Location", notNullValue()))
                .andReturn();
        String location = mvcResultWithLocation.getResponse().getHeader("Location");
        Assert.assertEquals(location, "http://localhost/customers/" + bruceBanner.getId() + "/photo");
        log.info("location: " + location);
    }

    @Test
    public void nonBlockingPhotoUploadWithNonExistingCustomer() throws Exception {
        MvcResult mvcResult = mockMvc.perform(multipart(urlTemplate, 0).file("file", dogeBytes))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, vndErrorMediaType.toString()))
                .andReturn();
    }

    @Test
    public void photoDownloadWithExistingPhoto() throws Exception {
        MvcResult mvcResult = mockMvc.perform(multipart(urlTemplate, bruceBanner.getId()).file("file", dogeBytes))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        mockMvc.perform(get(urlTemplate, bruceBanner.getId()).accept(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    public void photoDownloadWithNonExistingPhoto() throws Exception {
        MvcResult mvcResult = mockMvc
                .perform(get(urlTemplate, peterParker.getId()))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, vndErrorMediaType.toString()))
                .andReturn();

        log.info(mvcResult.getResponse().getContentAsString());

        mockMvc.perform(get(urlTemplate, 0))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, vndErrorMediaType.toString()))
                .andReturn();
    }

    @Configuration
    public static class EnvironmentConfiguration {

        @Autowired
        public void configureEnvironment(ConfigurableWebApplicationContext webApplicationContext) {

            PropertySource<Object> propertySource = new PropertySource<Object>("uploads") {
                @Override
                public Object getProperty(String name) {
                    if (name.equals("upload.dir")) {
                        return tmpFile.getAbsolutePath();
                    }
                    return null;
                }
            };

            webApplicationContext.getEnvironment().getPropertySources().addLast(propertySource);
        }
    }

}
