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
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait BaseDesConnector {
  val http: HttpClient
  val appConfig: AppConfig

  val logger: Logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
      .withExtraHeaders(headers = "Environment" -> appConfig.desEnv)


  /* TODO: Each of the HTTP methods accept an extra parameter called "headers" which will add the headers provided
  to the `extraHeaders` field of the headerCarrier. For any headers that aren't in the `extraHeaders` field of the
  headerCarrier to be sent to external source (i.e DES) they must MANUALLY be added to the HTTP call in this way.


  i.e  http.POST(url = s"${appConfig.desBaseUrl}/${uri.value}", body, Seq("authorisation" -> "Bearer t0k3n"))
  * */

  def post[Body: Writes, Resp](body: Body, uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                              hc: HeaderCarrier,
                                                              httpReads: HttpReads[DesOutcome[Resp]],
                                                              correlationId: String): Future[DesOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.POST(url = s"${appConfig.desBaseUrl}/${uri.value}", body)
    }

    doPost(desHeaderCarrier(hc.withExtraHeaders(headers = "CorrelationId" -> correlationId)))
  }

  def get[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                   hc: HeaderCarrier,
                                   httpReads: HttpReads[DesOutcome[Resp]],
                                   correlationId: String): Future[DesOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] =
      http.GET(url = s"${appConfig.desBaseUrl}/${uri.value}", queryParams = Seq.empty, headers = Seq.empty)

    doGet(desHeaderCarrier(hc.withExtraHeaders(headers = "CorrelationId" -> correlationId)))
  }

  def delete[Resp](uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                      hc: HeaderCarrier,
                                      httpReads: HttpReads[DesOutcome[Resp]],
                                      correlationId: String): Future[DesOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.DELETE(url = s"${appConfig.desBaseUrl}/${uri.value}")
    }

    doDelete(desHeaderCarrier(hc.withExtraHeaders(headers = "CorrelationId" -> correlationId)))
  }

  def put[Body: Writes, Resp](body: Body, uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                             hc: HeaderCarrier,
                                                             httpReads: HttpReads[DesOutcome[Resp]],
                                                             correlationId: String): Future[DesOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.PUT(url = s"${appConfig.desBaseUrl}/${uri.value}", body)
    }

    doPut(desHeaderCarrier(hc.withExtraHeaders(headers = "CorrelationId" -> correlationId)))
  }
}