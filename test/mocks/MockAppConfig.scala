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

package mocks

import config.{ AppConfig, ConfidenceLevelConfig }
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.Configuration

trait MockAppConfig extends MockFactory {

  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockAppConfig {
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
    def featureSwitches: CallHandler[Configuration]                     = (() => mockAppConfig.featureSwitches: Configuration).expects()
    def apiGatewayContext: CallHandler[String]                          = (() => mockAppConfig.apiGatewayContext: String).expects()
    def apiStatus: CallHandler[String]                                  = (mockAppConfig.apiStatus: String => String).expects("1.0")
    def endpointsEnabled: CallHandler[Boolean]                          = (mockAppConfig.endpointsEnabled: String => Boolean).expects("1.0")
    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] = (() => mockAppConfig.confidenceLevelConfig: ConfidenceLevelConfig).expects()
  }
}
