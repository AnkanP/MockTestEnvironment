package org.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.shaded.org.apache.commons.lang3.StringUtils.trim;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/**
 * Unit test for simple App.
 */

@Testcontainers
public class AppTest 
{

    // will be started before and stopped after each test method

    @ClassRule
    public  static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @ClassRule
    public  static DockerComposeContainer webServer = new DockerComposeContainer(
        new File("src/test/resources/docker-compose.yml"))
        .withExposedService("simpleWebServer", 80)
            .waitingFor("simpleWebServer", Wait.forHttp("/")
                    .forStatusCode(200));


    @ClassRule
    public  static DockerComposeContainer zooKeeper = new DockerComposeContainer(
            new File("src/test/resources/docker-compose.yml"))
            .withExposedService("kafka-connect",8083);

    //@ClassRule
    //public  static DockerComposeContainer broker = new DockerComposeContainer(
    //        new File("src/test/resources/docker-compose.yml"))
    //        .withExposedService("broker",9092);
//
//
    //@ClassRule
    //public  static DockerComposeContainer schemaRegistry = new DockerComposeContainer(
    //        new File("src/test/resources/docker-compose.yml"))
    //        .withExposedService("schema-registry",8089);

    @Test
    public void checkHealth() throws URISyntaxException, IOException, InterruptedException {
        System.out.println("SERVICE NAME: " + zooKeeper.getContainerByServiceName("kafka-connect").get().toString());
        System.out.println("SERVICE HOST: " + zooKeeper.getServiceHost("kafka-connect",8083).toString());
        System.out.println("SERVICE PORT: " + zooKeeper.getServicePort("kafka-connect",8083).toString());



        String address = "http://" + zooKeeper.getServiceHost("kafka-connect", 8083) + ":" + zooKeeper.getServicePort("kafka-connect", 8083) + "/connector-plugins";

        /** HTTP request */
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(address))
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .build();

        /** HTTP Client & Response */
        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("SERVICE RESPONSE: " + response.body().trim());

        assertTrue( true );
    }

    @Test
    public void givenSimpleWebServerContainer_whenGetReuqest_thenReturnsResponse()
            throws Exception {

        String address = "http://" + webServer.getServiceHost("simpleWebServer_1", 80) + ":" + webServer.getServicePort("simpleWebServer_1", 80);

        /** HTTP request */
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(address))
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .build();

        /** HTTP Client & Response */
        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());


        assertEquals("Hello World!", trim(response.body()));
    }


    @Test
    public void whenSelectQueryExecuted_thenResulstsReturned()
            throws Exception {
        String jdbcUrl = postgresqlContainer.getJdbcUrl();
        String username = postgresqlContainer.getUsername();
        String password = postgresqlContainer.getPassword();
        Connection conn = DriverManager
                .getConnection(jdbcUrl, username, password);
        ResultSet resultSet =
                conn.createStatement().executeQuery("SELECT 1");
        resultSet.next();
        int result = resultSet.getInt(1);

        assertEquals(1, result);
    }

    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

}
