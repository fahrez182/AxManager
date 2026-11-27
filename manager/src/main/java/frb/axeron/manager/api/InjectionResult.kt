package frb.axeron.manager.api

sealed interface InjectionResult {
  data class Success(val data: String) : InjectionResult
  data class Failure(val reason: String) : InjectionResult
}
