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

import api.connectors.DownstreamUri._
import config.{AppConfig, FeatureSwitches}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}
import utils.{Logging, UrlUtils}

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector extends Logging {
  val http: HttpClientV2
  val appConfig: AppConfig

  implicit protected lazy val featureSwitches: FeatureSwitches = FeatureSwitches(appConfig.featureSwitches)

  private val jsonContentTypeHeader = HeaderNames.CONTENT_TYPE -> MimeTypes.JSON

  def post[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.post(url"${getBackendUri(uri)}").withBody(Json.toJson(body)).execute

    doPost(getBackendHeaders(uri, hc, correlationId, jsonContentTypeHeader))
  }

  def get[Resp](uri: DownstreamUri[Resp], queryParams: Seq[(String, String)] = Seq.empty)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      val fullUrl: String = UrlUtils.appendQueryParams(getBackendUri(uri), queryParams)
      http.get(url"$fullUrl").execute
    }

    doGet(getBackendHeaders(uri, hc, correlationId))
  }

  def delete[Resp](uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.delete(url"${getBackendUri(uri)}").execute

    doDelete(getBackendHeaders(uri, hc, correlationId))
  }

  def put[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      httpReads: HttpReads[DownstreamOutcome[Resp]],
      correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.put(url"${getBackendUri(uri)}").withBody(Json.toJson(body)).execute

    doPut(getBackendHeaders(uri, hc, correlationId, jsonContentTypeHeader))
  }

  private def getBackendUri[Resp](uri: DownstreamUri[Resp]): String =
    s"${configFor(uri).baseUrl}/${uri.value}"

  private def configFor[Resp](uri: DownstreamUri[Resp]) =
    uri match {
      case Ifs1Uri(_) => appConfig.ifs1DownstreamConfig
      case Ifs2Uri(_) => appConfig.ifs2DownstreamConfig
    }

  private def getBackendHeaders[Resp](uri: DownstreamUri[Resp],
                                      hc: HeaderCarrier,
                                      correlationId: String,
                                      additionalHeaders: (String, String)*): HeaderCarrier = {
    val downstreamConfig = configFor(uri)

    val passThroughHeaders = hc
      .headers(downstreamConfig.environmentHeaders.getOrElse(Seq.empty))
      .filterNot(hdr => additionalHeaders.exists(_._1.equalsIgnoreCase(hdr._1)))

    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${downstreamConfig.token}",
          "Environment"   -> downstreamConfig.env,
          "CorrelationId" -> correlationId
        ) ++
        additionalHeaders ++
        passThroughHeaders
    )
  }

}
