/*
 * Copyright 2023 HM Revenue & Customs
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

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import routing.Version
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

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

  // Business Rule Config
  def minimumPermittedTaxYear: Int

  // API Config
  def apiGatewayContext: String
  def confidenceLevelConfig: ConfidenceLevelConfig
  def apiStatus(version: Version): String
  def isApiDeprecated(version: Version): Boolean
  def featureSwitches: Configuration
  def endpointsEnabled(version: Version): Boolean
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig, configuration: Configuration) extends AppConfig {
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

  // Business rule Config
  val minimumPermittedTaxYear: Int = config.getInt("minimumPermittedTaxYear")

  // API Config
  val apiGatewayContext: String                    = config.getString("api.gateway.context")
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")
  def apiStatus(version: Version): String          = config.getString(s"api.${version.name}.status")
  def isApiDeprecated(version: Version): Boolean   = apiStatus(version) == "DEPRECATED"
  def featureSwitches: Configuration               = configuration.getOptional[Configuration](s"feature-switch").getOrElse(Configuration.empty)
  def endpointsEnabled(version: Version): Boolean  = config.getBoolean(s"api.${version.name}.endpoints.enabled")
}

trait FixedConfig {
  // Minimum tax year for MTD
  val minimumTaxYear = 2018
}

case class ConfidenceLevelConfig(confidenceLevel: ConfidenceLevel, definitionEnabled: Boolean, authValidationEnabled: Boolean)

object ConfidenceLevelConfig {

  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      confidenceLevel = ConfidenceLevel.fromInt(config.getInt("confidence-level")).getOrElse(ConfidenceLevel.L200),
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }

}
