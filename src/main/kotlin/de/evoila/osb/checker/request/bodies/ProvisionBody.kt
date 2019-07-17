package de.evoila.osb.checker.request.bodies

import com.fasterxml.jackson.annotation.JsonInclude
import de.evoila.osb.checker.response.Plan
import de.evoila.osb.checker.response.Service
import java.util.*

abstract class ProvisionBody : RequestBody {

  data class ValidProvisioning(
      var service_id: String?,
      var plan_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString(),
      @JsonInclude(JsonInclude.Include.NON_NULL)
      var parameters: Map<String, Any>? = null
  ) : ProvisionBody() {

    constructor(service: Service, plan: Plan) : this(
        service_id = service.id,
        plan_id = plan.id
    )
  }

  data class NoPlanFieldProvisioning(
      var service_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(service: Service) : this(
        service_id = service.id
    )
  }

  data class NoServiceFieldProvisioning(
      var plan_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(plan: Plan) : this(
        plan_id = plan.id
    )
  }

  data class NoOrgFieldProvisioning(
      var service_id: String?,
      var plan_id: String?,
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(service: Service, plan: Plan) : this(
        service_id = service.id,
        plan_id = plan.id
    )
  }

  data class NoSpaceFieldProvisioning(
      var service_id: String?,
      var plan_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(service: Service, plan: Plan) : this(
        service_id = service.id,
        plan_id = plan.id
    )
  }
}