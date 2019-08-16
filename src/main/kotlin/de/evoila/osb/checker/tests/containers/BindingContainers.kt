package de.evoila.osb.checker.tests.containers

import de.evoila.osb.checker.request.BindingRequestRunner
import de.evoila.osb.checker.request.ProvisionRequestRunner
import de.evoila.osb.checker.request.bodies.BindingBody
import de.evoila.osb.checker.request.bodies.ProvisionBody
import de.evoila.osb.checker.response.catalog.Plan
import de.evoila.osb.checker.response.operations.AsyncProvision
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.springframework.stereotype.Service
import kotlin.test.assertTrue

@Service
class BindingContainers(
    val provisionRequestRunner: ProvisionRequestRunner,
    val bindingRequestRunner: BindingRequestRunner
) {

  fun validDeleteProvisionContainer(instanceId: String, service: de.evoila.osb.checker.response.catalog.Service, plan: Plan): DynamicContainer {
    return DynamicContainer.dynamicContainer("Deleting provision",
        listOf(
            DynamicTest.dynamicTest(DELETE_PROVISION_MESSAGE) {
              val response = provisionRequestRunner.runDeleteProvisionRequestAsync(instanceId, service.id, plan.id, intArrayOf(200, 202))

              if (response.statusCode() == 202) {
                val provision = response.jsonPath().getObject("", AsyncProvision::class.java)
                provisionRequestRunner.waitForFinish(instanceId, 410, provision.operation)
              }
            }
        ))
  }

  fun validBindingContainer(binding: BindingBody, instanceId: String, bindingId: String, isRetrievable: Boolean): DynamicContainer {

    return DynamicContainer.dynamicContainer(VALID_BINDING_MESSAGE, if (isRetrievable) {
      createValidBindingTests(bindingId, binding, instanceId)
          .plus(listOf(validRetrievableBindingContainer(instanceId, bindingId), validDeleteTest(binding, instanceId, bindingId)))
    } else {
      createValidBindingTests(bindingId, binding, instanceId).plus(validDeleteTest(binding, instanceId, bindingId))
    })
  }

  fun createValidBindingTests(bindingId: String, binding: BindingBody, instanceId: String): List<DynamicTest> {
    return listOf(
        DynamicTest.dynamicTest("Running a valid binding with bindingId $bindingId") {
          val statusCode = bindingRequestRunner.runPutBindingRequest(binding, instanceId, bindingId, 201, 202)

          if (statusCode == 202) {
            val state = bindingRequestRunner.waitForFinish(instanceId, bindingId, 200)
            assertTrue("Expected the final polling state to be \"succeeded\" but was $state") { "succeeded" == state }
          }
        }
    )
  }

  fun validDeleteTest(binding: BindingBody, instanceId: String, bindingId: String): DynamicTest =
      DynamicTest.dynamicTest("Deleting binding with bindingId $bindingId") {
        val statusCode = bindingRequestRunner.runDeleteBindingRequest(binding.service_id, binding.plan_id, instanceId, bindingId, 200, 202)

        if (statusCode == 202) {
          bindingRequestRunner.waitForFinish(instanceId, bindingId, 410)
        }
      }

  fun validRetrievableBindingContainer(instanceId: String, bindingId: String): DynamicTest {

    return DynamicTest.dynamicTest("Running valid GET for retrievable service binding") {
      bindingRequestRunner.runGetBindingRequest(200, instanceId, bindingId)
    }
  }

  fun validRetrievableInstanceContainer(instanceId: String, provision: ProvisionBody.ValidProvisioning, isRetrievable: Boolean): DynamicTest {

    return DynamicTest.dynamicTest("Running valid GET for retrievable service instance") {

      val serviceInstance = provisionRequestRunner.getProvision(instanceId, isRetrievable)

      assertTrue("When retrieving the instance the response did not match the expected value. \n" +
          "service_id: expected ${provision.service_id} actual ${serviceInstance.serviceId} \n" +
          "plan_id: expected ${provision.plan_id} actual ${serviceInstance.planId}")
      {
        serviceInstance.serviceId == provision.service_id && serviceInstance.planId == provision.plan_id
      }
    }
  }

  private fun createValidProvisionTests(instanceId: String, provision: ProvisionBody.ValidProvisioning, planName: String): List<DynamicTest> {
    return listOf(
        DynamicTest.dynamicTest("Running valid PUT provision with instanceId $instanceId for service ${provision.service_id} and plan $planName id: ${provision.plan_id}") {

          val response = provisionRequestRunner.runPutProvisionRequestAsync(instanceId, provision, 201, 202, 200)

          if (response.statusCode() == 202) {
            val provision = response.jsonPath().getObject("", AsyncProvision::class.java)

            val state = provisionRequestRunner.waitForFinish(instanceId, 200, provision.operation)
            assertTrue("Expected the final polling state to be \"succeeded\" but was $state") { "succeeded" == state }
          }
        })
  }

  fun validProvisionContainer(instanceId: String, planName: String, provision: ProvisionBody.ValidProvisioning, isRetrievable: Boolean): DynamicContainer {
    val provisionTests = createValidProvisionTests(instanceId, provision, planName)

    return DynamicContainer.dynamicContainer("Provision and in case of a async service broker polling, for later binding", if (isRetrievable) {
      provisionTests.plus(validRetrievableInstanceContainer(instanceId, provision, isRetrievable))
    } else {
      provisionTests
    })
  }

  companion object {
    private const val VALID_BINDING_MESSAGE = "Running PUT binding and DELETE binding afterwards"
    private const val DELETE_PROVISION_MESSAGE = "DELETE provision and if the service broker is async polling afterwards"
  }
}