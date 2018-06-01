package de.evoila.osb.checker.request.bodies

import de.evoila.osb.checker.response.Plan
import de.evoila.osb.checker.response.Service
import java.util.*

abstract class ProvisionBody : RequestBody {

  class ValidProvisioning(
      var service_id: String?,
      var plan_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString(),
      var parameters: Map<String, String>? = null
  ) : ProvisionBody() {

    constructor(service: Service, plan: Plan) : this(
        service_id = service.id,
        plan_id = plan.id
    )
  }

  class NoServiceFieldProvisioning(
      var service_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(service: Service) : this(
        service_id = service.id
    )
  }

  class NoPlanFieldProvisioning(
      var plan_id: String?,
      var organization_guid: String? = UUID.randomUUID().toString(),
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(plan: Plan) : this(
        plan_id = plan.id
    )
  }

  class NoOrgFieldProvisioning(
      var service_id: String?,
      var plan_id: String?,
      var space_guid: String? = UUID.randomUUID().toString()
  ) : ProvisionBody() {

    constructor(service: Service, plan: Plan) : this(
        service_id = service.id,
        plan_id = plan.id
    )
  }

  class NoSpaceFieldProvisioning(
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