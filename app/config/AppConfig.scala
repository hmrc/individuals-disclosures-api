/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import cats.data.Validated
import cats.implicits.catsSyntaxValidatedId
import com.typesafe.config.Config
import config.Deprecation.{Deprecated, NotDeprecated}
import play.api.{ConfigLoader, Configuration}
import routing.Version
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import javax.inject.{Inject, Singleton}

trait AppConfig {
  // MTD ID Lookup Config
  def mtdIdBaseUrl: String

  // IFS-1 Config
  def ifs1BaseUrl: String
  def ifs1Env: String
  def ifs1Token: String
  def ifs1EnvironmentHeaders: Option[Seq[String]]

  lazy val ifs1DownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = ifs1BaseUrl, env = ifs1Env, token = ifs1Token, environmentHeaders = ifs1EnvironmentHeaders)

  // IFS-2 Config
  def ifs2BaseUrl: String
  def ifs2Env: String
  def ifs2Token: String
  def ifs2EnvironmentHeaders: Option[Seq[String]]

  lazy val ifs2DownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = ifs2BaseUrl, env = ifs2Env, token = ifs2Token, environmentHeaders = ifs2EnvironmentHeaders)

  // HIP Config
  def hipBaseUrl: String
  def hipEnv: String
  def hipClientId: String
  def hipClientSecret: String
  def hipEnvironmentHeaders: Option[Seq[String]]

  lazy val hipDownstreamConfig: BasicAuthDownstreamConfig = BasicAuthDownstreamConfig(
    baseUrl = hipBaseUrl,
    env = hipEnv,
    clientId = hipClientId,
    clientSecret = hipClientSecret,
    environmentHeaders = hipEnvironmentHeaders
  )

  // Business Rule Config
  def minimumPermittedTaxYear: Int

  // API Config
  def apiGatewayContext: String
  def confidenceLevelConfig: ConfidenceLevelConfig
  def apiStatus(version: Version): String
  def featureSwitches: Configuration
  def endpointsEnabled(version: Version): Boolean
  def endpointsEnabled(version: String): Boolean

  def apiDocumentationUrl: String

  def deprecationFor(version: Version): Validated[String, Deprecation]

  def apiVersionReleasedInProduction(version: String): Boolean

  def endpointReleasedInProduction(version: String, name: String): Boolean
  def safeEndpointsEnabled(version: String): Boolean

  /** Defaults to false
    */
  def endpointAllowsSupportingAgents(endpointName: String): Boolean
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig, val configuration: Configuration) extends AppConfig {
  // API name
  val appName: String = config.getString("appName")

  // MTD ID Lookup Config
  val mtdIdBaseUrl: String = config.baseUrl(serviceName = "mtd-id-lookup")

  // IFS-1 Config
  val ifs1BaseUrl: String                         = config.baseUrl("ifs1")
  val ifs1Env: String                             = config.getString("microservice.services.ifs1.env")
  val ifs1Token: String                           = config.getString("microservice.services.ifs1.token")
  val ifs1EnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifs1.environmentHeaders")

  // IFS-2 Config
  val ifs2BaseUrl: String                         = config.baseUrl("ifs2")
  val ifs2Env: String                             = config.getString("microservice.services.ifs2.env")
  val ifs2Token: String                           = config.getString("microservice.services.ifs2.token")
  val ifs2EnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifs2.environmentHeaders")

  // HIP Config
  val hipBaseUrl: String                         = config.baseUrl("hip")
  val hipEnv: String                             = config.getString("microservice.services.hip.env")
  val hipClientId: String                        = config.getString("microservice.services.hip.clientId")
  val hipClientSecret: String                    = config.getString("microservice.services.hip.clientSecret")
  val hipEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.hip.environmentHeaders")

  // Business rule Config
  val minimumPermittedTaxYear: Int = config.getInt("minimumPermittedTaxYear")

  // API Config
  val apiGatewayContext: String                    = config.getString("api.gateway.context")
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")
  def apiStatus(version: Version): String          = config.getString(s"api.${version.name}.status")
  def featureSwitches: Configuration               = configuration.getOptional[Configuration](s"feature-switch").getOrElse(Configuration.empty)

  def endpointsEnabled(version: Version): Boolean = config.getBoolean(s"api.${version.name}.endpoints.enabled")
  def endpointsEnabled(version: String): Boolean  = config.getBoolean(s"api.$version.endpoints.enabled")

  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 23)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 59)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 59)
    .toFormatter()

  def deprecationFor(version: Version): Validated[String, Deprecation] = {
    val isApiDeprecated: Boolean = apiStatus(version) == "DEPRECATED"

    val deprecatedOn: Option[LocalDateTime] =
      configuration
        .getOptional[String](s"api.$version.deprecatedOn")
        .map(value => LocalDateTime.parse(value, DATE_FORMATTER))

    val sunsetDate: Option[LocalDateTime] =
      configuration
        .getOptional[String](s"api.$version.sunsetDate")
        .map(value => LocalDateTime.parse(value, DATE_FORMATTER))

    val isSunsetEnabled: Boolean =
      configuration.getOptional[Boolean](s"api.$version.sunsetEnabled").getOrElse(true)

    if (isApiDeprecated) {
      (deprecatedOn, sunsetDate, isSunsetEnabled) match {
        case (Some(dO), Some(sD), true) =>
          if (sD.isAfter(dO))
            Deprecated(dO, Some(sD)).valid
          else
            s"sunsetDate must be later than deprecatedOn date for a deprecated version $version".invalid
        case (Some(dO), None, true) => Deprecated(dO, Some(dO.plusMonths(6))).valid
        case (Some(dO), _, false)   => Deprecated(dO, None).valid
        case _                      => s"deprecatedOn date is required for a deprecated version $version".invalid
      }

    } else NotDeprecated.valid

  }

  def apiVersionReleasedInProduction(version: String): Boolean = config.getBoolean(s"api.$version.endpoints.api-released-in-production")

  def apiDocumentationUrl: String =
    configuration
      .getOptional[String]("api.documentation-url")
      .getOrElse(s"https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/$appName")

  def safeEndpointsEnabled(version: String): Boolean =
    configuration
      .getOptional[Boolean](s"api.$version.endpoints.enabled")
      .getOrElse(false)

  def endpointAllowsSupportingAgents(endpointName: String): Boolean =
    supportingAgentEndpoints.getOrElse(endpointName, false)

  private val supportingAgentEndpoints: Map[String, Boolean] =
    configuration
      .getOptional[Map[String, Boolean]]("api.supporting-agent-endpoints")
      .getOrElse(Map.empty)

  def endpointReleasedInProduction(version: String, name: String): Boolean = {
    val versionReleasedInProd = apiVersionReleasedInProduction(version)
    val path                  = s"api.$version.endpoints.released-in-production.$name"

    val conf = configuration.underlying
    if (versionReleasedInProd && conf.hasPath(path)) config.getBoolean(path) else versionReleasedInProd
  }

}

case class ConfidenceLevelConfig(confidenceLevel: ConfidenceLevel, definitionEnabled: Boolean, authValidationEnabled: Boolean)

object ConfidenceLevelConfig {

  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      confidenceLevel = ConfidenceLevel.fromInt(config.getInt("confidence-level")).getOrElse(ConfidenceLevel.L250),
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }

}
