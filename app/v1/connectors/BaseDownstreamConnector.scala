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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v1.models.errors.DownstreamError

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector {
  val http: HttpClient
  val appConfig: AppConfig

  val logger: Logger = Logger(this.getClass)

  private def downstreamHeaderCarrier(additionalHeaders: Seq[String] = Seq.empty)(implicit hc: HeaderCarrier,
                                                                                  correlationId: String): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.desToken}",
          "Environment" -> appConfig.desEnv,
          "CorrelationId" -> correlationId
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.desEnvironmentHeaders.getOrElse(Seq.empty))
    )

  def get[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                   hc: HeaderCarrier,
                                   httpReads: HttpReads[DesOutcome[Resp]],
                                   correlationId: String): Future[DesOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] =
      http.GET(url = s"${appConfig.desBaseUrl}/${uri.value}")

    doGet(downstreamHeaderCarrier())
  }

  def delete[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                      hc: HeaderCarrier,
                                      httpReads: HttpReads[DesOutcome[Resp]],
                                      correlationId: String): Future[DesOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.DELETE(url = s"${appConfig.desBaseUrl}/${uri.value}")
    }

    doDelete(downstreamHeaderCarrier())
  }

  def put[Body: Writes, Resp](body: Body, uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                             hc: HeaderCarrier,
                                                             httpReads: HttpReads[DesOutcome[Resp]],
                                                             correlationId: String): Future[DesOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.PUT(url = s"${appConfig.desBaseUrl}/${uri.value}", body)
    }

    doPut(downstreamHeaderCarrier(Seq("Content-Type")))
  }

  def post[Body: Writes, Resp](body: Body, uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                                     hc: HeaderCarrier,
                                                                     httpReads: HttpReads[DesOutcome[Resp]],
                                                                     correlationId: String): Future[DesOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.POST(url = s"${appConfig.desBaseUrl}/${uri.value}", body)
    }

    doPost(downstreamHeaderCarrier(Seq("Content-Type")))
  }
}
