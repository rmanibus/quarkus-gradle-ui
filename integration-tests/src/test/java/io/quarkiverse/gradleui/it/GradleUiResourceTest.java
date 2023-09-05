package io.quarkiverse.gradleui.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GradleUiResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/gradle-ui")
                .then()
                .statusCode(200)
                .body(is("Hello gradle-ui"));
    }
}
