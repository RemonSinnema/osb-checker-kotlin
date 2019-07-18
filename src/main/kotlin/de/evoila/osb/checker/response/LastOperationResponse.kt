package de.evoila.osb.checker.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class LastOperationResponse(
    val state: State,
    val description: String?
) {

  enum class State {
    @JsonProperty(value = "in progress")
    IN_PROGRESS,
    @JsonProperty(value = "succeeded")
    SUCCEEDED,
    @JsonProperty(value = "failed")
    FAILED,
    GONE
  }
}