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

package api.connectors

import config.AppConfig

case class DownstreamUri[+Resp](path: String, strategy: DownstreamStrategy)

object DownstreamUri {

  def Ifs1Uri[Resp](path: String)(implicit appConfig: AppConfig): DownstreamUri[Resp] =
    DownstreamUri(path, DownstreamStrategy.standardStrategy(appConfig.ifs1DownstreamConfig))

  def Ifs2Uri[Resp](path: String)(implicit appConfig: AppConfig): DownstreamUri[Resp] =
    DownstreamUri(path, DownstreamStrategy.standardStrategy(appConfig.ifs2DownstreamConfig))

  def HipUri[Resp](path: String)(implicit appConfig: AppConfig): DownstreamUri[Resp] =
    DownstreamUri(path, DownstreamStrategy.basicAuthStrategy(appConfig.hipDownstreamConfig))

}
