/*
 * Copyright 2022 HM Revenue & Customs
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

package v1.services

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1.connectors.DeleteRetrieveConnector
import v1.connectors.DownstreamUri.Ifs1Uri
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.support.DownstreamResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteRetrieveService @Inject()(connector: DeleteRetrieveConnector) extends DownstreamResponseMappingSupport with Logging {

  def delete(downstreamErrorMap: Map[String, MtdError] = defaultDownstreamErrorMap)(implicit hc: HeaderCarrier,
               ec: ExecutionContext,
               logContext: EndpointLogContext,
                                                                      ifs1Uri: Ifs1Uri[Unit],
               correlationId: String): Future[Either[ErrorWrapper, ResponseWrapper[Unit]]] = {

    val result = for {
      downstreamResponseWrapper <- EitherT(connector.delete()).leftMap(mapDownstreamErrors(downstreamErrorMap))
    } yield downstreamResponseWrapper

    result.value
  }

  def retrieve[Resp: Format](downstreamErrorMap: Map[String, MtdError] = defaultDownstreamErrorMap)(implicit hc: HeaderCarrier,
                               ec: ExecutionContext,
                               logContext: EndpointLogContext,
                                                                                      ifs1Uri: Ifs1Uri[Resp],
                               correlationId: String): Future[Either[ErrorWrapper, ResponseWrapper[Resp]]] = {

    val result = for {
      downstreamResponseWrapper <- EitherT(connector.retrieve[Resp]()).leftMap(mapDownstreamErrors(downstreamErrorMap))
      mtdResponseWrapper <- EitherT.fromEither[Future](validateRetrieveResponse(downstreamResponseWrapper))
    } yield mtdResponseWrapper

    result.value
  }

  private def defaultDownstreamErrorMap: Map[String, MtdError] = {
    val error = Map(
      "INVALID_NINO"          -> NinoFormatError,
      "INVALID_TAX_YEAR"      -> TaxYearFormatError,
      "INVALID_CORRELATIONID" -> InternalError,
      "NO_DATA_FOUND"         -> NotFoundError,
      "SERVER_ERROR"          -> InternalError,
      "SERVICE_UNAVAILABLE"   -> InternalError
    )

    val extra_error = Map {
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError
    }
    error ++ extra_error
  }
}