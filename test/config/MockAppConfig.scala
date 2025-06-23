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

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.Configuration
import routing.Version

trait MockAppConfig extends TestSuite with MockFactory {

  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {
    // MTD ID Lookup Config
    def mtdIdBaseUrl: CallHandler[String] = (() => mockAppConfig.mtdIdBaseUrl: String).expects()

    // IFS-1 Config
    def ifs1BaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifs1BaseUrl: String).expects()
    def ifs1Token: CallHandler[String]                           = (() => mockAppConfig.ifs1Token: String).expects()
    def ifs1Environment: CallHandler[String]                     = (() => mockAppConfig.ifs1Env: String).expects()
    def ifs1EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifs1EnvironmentHeaders: Option[Seq[String]]).expects()

    // IFS-2 Config
    def ifs2BaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifs2BaseUrl: String).expects()
    def ifs2Token: CallHandler[String]                           = (() => mockAppConfig.ifs2Token: String).expects()
    def ifs2Environment: CallHandler[String]                     = (() => mockAppConfig.ifs2Env: String).expects()
    def ifs2EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifs2EnvironmentHeaders: Option[Seq[String]]).expects()

    // Business Rule Config
    def minimumPermittedTaxYear: CallHandler[Int] = (() => mockAppConfig.minimumPermittedTaxYear: Int).expects()

    // API Config
    def featureSwitches: CallHandler[Configuration]                           = (() => mockAppConfig.featureSwitches: Configuration).expects()
    def apiGatewayContext: CallHandler[String]                                = (() => mockAppConfig.apiGatewayContext: String).expects()
    def apiStatus(version: Version): CallHandler[String]                      = (mockAppConfig.apiStatus(_: Version)).expects(version)
    def apiVersionReleasedInProduction(version: String): CallHandler[Boolean] =
      (mockAppConfig.apiVersionReleasedInProduction: String => Boolean).expects(version)
    def endpointsEnabled(version: String): CallHandler[Boolean]               = (mockAppConfig.endpointsEnabled(_: String)).expects(version)
    def endpointsEnabled(version: Version): CallHandler[Boolean]              = (mockAppConfig.endpointsEnabled(_: Version)).expects(version)
    def confidenceLevelConfig: CallHandler[ConfidenceLevelConfig]             = (() => mockAppConfig.confidenceLevelConfig: ConfidenceLevelConfig).expects()

    def endpointReleasedInProduction(version: String, key: String): CallHandler[Boolean] =
      (mockAppConfig.endpointReleasedInProduction: (String, String) => Boolean).expects(version, key)
    def endpointAllowsSupportingAgents(endpointName: String): CallHandler[Boolean] =
      (mockAppConfig.endpointAllowsSupportingAgents(_: String)).expects(endpointName)

  }

}
