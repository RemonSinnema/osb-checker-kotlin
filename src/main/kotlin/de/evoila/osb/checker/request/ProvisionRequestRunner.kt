package de.evoila.osb.checker.request

import de.evoila.osb.checker.config.Configuration
import de.evoila.osb.checker.request.bodies.RequestBody
import de.evoila.osb.checker.response.operations.LastOperationResponse
import de.evoila.osb.checker.response.catalog.ServiceInstance
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.http.Header
import io.restassured.module.jsv.JsonSchemaValidator
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import org.hamcrest.collection.IsIn
import org.springframework.stereotype.Service
import kotlin.test.assertTrue

@Service
class ProvisionRequestRunner(
        val configuration: Configuration
) {

    fun getProvision(instanceId: String, retrievable: Boolean): ServiceInstance {
        return RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)
                .get("/v2/service_instances/$instanceId")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .and()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_FETCH_INSTANCE_BODY))
                .extract()
                .response()
                .jsonPath()
                .getObject("", ServiceInstance::class.java)
    }

    fun runPutProvisionRequestSync(instanceId: String, requestBody: RequestBody) {
        val response = RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/v2/service_instances/$instanceId")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(IsIn(listOf(201, 422)))
                .and()
                .extract()

        if (response.statusCode() == 201) {
            JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_PROVISION_RESPONSE).matches(response.body())
        }
    }

    fun runPutProvisionRequestAsync(instanceId: String, requestBody: RequestBody, vararg expectedFinalStatusCodes: Int): ExtractableResponse<Response> {
        val response = RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .put("/v2/service_instances/$instanceId?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .statusCode(IsIn(expectedFinalStatusCodes.asList()))
                .assertThat()
                .extract()

        if (response.statusCode() in listOf(201, 202, 200)) {
            JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_PROVISION_RESPONSE).matches(response.body())
        }

        return response
    }

    fun waitForFinish(instanceId: String, expectedFinalStatusCode: Int, operationData: String?): String {
        val request = RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)

        operationData?.let { request.queryParam("operation", it) }

        val response = request
                .get("/v2/service_instances/$instanceId/last_operation")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(IsIn(listOf(expectedFinalStatusCode, 200)))
                .extract()
                .response()

        return if (response.statusCode == 200) {

            val responseBody = response.jsonPath()
                    .getObject("", LastOperationResponse::class.java)

            JsonSchemaValidator.matchesJsonSchemaInClasspath("polling-response-schema.json").matches(responseBody)

            if (responseBody.state == "in progress") {
                Thread.sleep(10000)
                return waitForFinish(instanceId, expectedFinalStatusCode, operationData)
            }
            assertTrue("Expected response body \"succeeded\" or \"failed\" but was ${responseBody.state}")
            { responseBody.state in listOf("succeeded", "failed") }

            responseBody.state
        } else {
            ""
        }
    }

    fun runDeleteProvisionRequestSync(instanceId: String, serviceId: String?, planId: String?) {
        var path = "/v2/service_instances/$instanceId"
        path = serviceId?.let { "$path?service_id=$serviceId" } ?: path
        path = planId?.let { "$path&plan_id=$planId" } ?: path

        val response = RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)
                .delete(path)
                .then()
                .log().ifValidationFails()
                .statusCode(IsIn(listOf(200, 422)))
                .extract()

        if (response.statusCode() != 200) {
            JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE).matches(response.body())
        }
    }

    fun runDeleteProvisionRequestAsync(instanceId: String, serviceId: String?, planId: String?, expectedFinalStatusCodes: IntArray): ExtractableResponse<Response> {
        var path = "/v2/service_instances/$instanceId?accepts_incomplete=true"
        path = serviceId?.let { "$path&service_id=$serviceId" } ?: path
        path = planId?.let { "$path&plan_id=$planId" } ?: path

        return RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .header(Header("Authorization", configuration.correctToken))
                .contentType(ContentType.JSON)
                .delete(path)
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(IsIn(expectedFinalStatusCodes.asList()))
                .extract()
    }

    fun putWithoutHeader() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.correctToken))
                .put("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(412)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun deleteWithoutHeader() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.correctToken))
                .delete("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true&service_id=Invalid&plan_id=Invalid")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(412)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun lastOperationWithoutHeader() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.correctToken))
                .get("/v2/service_instances/${Configuration.notAnId}/last_operation")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(412)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun putNoAuth() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .put("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun putWrongUser() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongUserToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .put("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun putWrongPassword() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongPasswordToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .put("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun deleteNoAuth() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .delete("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun deleteWrongUser() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongUserToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .delete("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun deleteWrongPassword() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongPasswordToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .delete("/v2/service_instances/${Configuration.notAnId}?accepts_incomplete=true")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun lastOpNoAuth() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .get("/v2/service_instances/${Configuration.notAnId}/last_operation")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun lastOpWrongUser() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongUserToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .get("/v2/service_instances/${Configuration.notAnId}/last_operation")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    fun lastOpWrongPassword() {
        RestAssured.with()
                .log().ifValidationFails()
                .header(Header("Authorization", configuration.wrongPasswordToken))
                .header(Header("X-Broker-API-Version", "${configuration.apiVersion}"))
                .contentType(ContentType.JSON)
                .get("/v2/service_instances/${Configuration.notAnId}/last_operation")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(401)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(PATH_ERROR_CODE))
    }

    companion object {
        private const val PATH_ERROR_CODE = "service-broker-error-response.json"
        private const val PATH_PROVISION_RESPONSE = "provision-response-schema.json"
        private const val PATH_FETCH_INSTANCE_BODY = "fetch-instance-response-schema.json"
    }
}