/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.connectors

import config.AppConfig
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads }

import scala.concurrent.{ ExecutionContext, Future }

trait BaseDownstreamConnector {
  val http: HttpClient
  val appConfig: AppConfig

  val logger: Logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier, correlationId: String): HeaderCarrier =
    HeaderCarrier(
     extraHeaders = hc.extraHeaders ++
       Seq(
         "Authorization" -> s"Bearer ${appConfig.desToken}",
         "Environment" -> appConfig.desEnv,
         "CorrelationId" -> correlationId
       ) ++
       appConfig.desEnvironmentHeaders.fold(Seq.empty[(String, String)])(
         headers => hc.headers(headers)
       )
    )

  def get[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                   hc: HeaderCarrier,
                                   httpReads: HttpReads[DesOutcome[Resp]],
                                   correlationId: String): Future[DesOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] =
      http.GET(url = s"${appConfig.desBaseUrl}/${uri.value}")

    doGet(desHeaderCarrier(hc, correlationId))
  }

  def delete[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                      hc: HeaderCarrier,
                                      httpReads: HttpReads[DesOutcome[Resp]],
                                      correlationId: String): Future[DesOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.DELETE(url = s"${appConfig.desBaseUrl}/${uri.value}")
    }

    doDelete(desHeaderCarrier(hc, correlationId))
  }

  def put[Body: Writes, Resp](body: Body, uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                             hc: HeaderCarrier,
                                                             httpReads: HttpReads[DesOutcome[Resp]],
                                                             correlationId: String): Future[DesOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.PUT(url = s"${appConfig.desBaseUrl}/${uri.value}", body)
    }

    doPut(desHeaderCarrier(hc, correlationId))
  }
}
