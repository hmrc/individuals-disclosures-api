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
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}
import utils.{Logging, UrlUtils}

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector extends Logging {
  val http: HttpClientV2
  val appConfig: AppConfig

  implicit protected lazy val _appConfig: AppConfig = appConfig

  private val jsonContentTypeHeader: Seq[(String, String)] = Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

  def post[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy: DownstreamStrategy = uri.strategy

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.post(url"${getBackendUri(uri.path, strategy)}").withBody(Json.toJson(body)).execute

    for {
      headers <- getBackendHeaders(strategy, jsonContentTypeHeader)
      result  <- doPost(headers)
    } yield result
  }

  def get[Resp](uri: DownstreamUri[Resp], queryParams: Seq[(String, String)] = Seq.empty)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy: DownstreamStrategy = uri.strategy

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      val fullUrl: String = UrlUtils.appendQueryParams(getBackendUri(uri.path, strategy), queryParams)
      http.get(url"$fullUrl").execute
    }

    for {
      headers <- getBackendHeaders(strategy)
      result  <- doGet(headers)
    } yield result
  }

  def delete[Resp](uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy: DownstreamStrategy = uri.strategy

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.delete(url"${getBackendUri(uri.path, strategy)}").execute

    for {
      headers <- getBackendHeaders(strategy)
      result  <- doDelete(headers)
    } yield result
  }

  def put[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    val strategy: DownstreamStrategy = uri.strategy

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.put(url"${getBackendUri(uri.path, strategy)}").withBody(Json.toJson(body)).execute

    for {
      headers <- getBackendHeaders(strategy, jsonContentTypeHeader)
      result  <- doPut(headers)
    } yield result
  }

  private def getBackendUri(path: String, strategy: DownstreamStrategy): String = s"${strategy.baseUrl}/$path"

  private def getBackendHeaders(strategy: DownstreamStrategy,
                                additionalHeaders: Seq[(String, String)] = Seq.empty)(implicit
                                                                                      ec: ExecutionContext,
                                                                                      hc: HeaderCarrier,
                                                                                      correlationId: String): Future[HeaderCarrier] = {

    for {
      contractHeaders <- strategy.contractHeaders(correlationId)
    } yield {
      val apiHeaders: Seq[(String, String)] = hc.extraHeaders ++ contractHeaders ++ additionalHeaders

      val passThroughHeaders: Seq[(String, String)] = hc
        .headers(strategy.environmentHeaders)
        .filterNot(hdr => apiHeaders.exists(_._1.equalsIgnoreCase(hdr._1)))

      HeaderCarrier(extraHeaders = apiHeaders ++ passThroughHeaders)
    }

  }

}
