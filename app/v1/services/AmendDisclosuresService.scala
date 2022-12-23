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
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1.connectors.AmendDisclosuresConnector
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.disclosures.AmendDisclosuresRequest
import v1.support.DownstreamResponseMappingSupport

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class AmendDisclosuresService @Inject()(connector: AmendDisclosuresConnector) extends DownstreamResponseMappingSupport with Logging {

  def amendDisclosures(request: AmendDisclosuresRequest)(implicit hc: HeaderCarrier,
                                                         ec: ExecutionContext,
                                                         logContext: EndpointLogContext,
                                                         correlationId: String): Future[Either[ErrorWrapper, ResponseWrapper[Unit]]] = {

    val result = for {
      downstreamResponseWrapper <- EitherT(connector.amendDisclosures(request)).leftMap(mapDownstreamErrors(ifsErrorMap))
    } yield downstreamResponseWrapper

    result.value
  }

  private def ifsErrorMap = {
    val error = Map(
      "INVALID_NINO"                       -> NinoFormatError,
      "INVALID_TAX_YEAR"                   -> TaxYearFormatError,
      "INVALID_CORRELATIONID"              -> InternalError,
      "INVALID_PAYLOAD"                    -> InternalError,
      "INCOME_SOURCE_NOT_FOUND"            -> NotFoundError,
      "VOLUNTARY_CLASS2_CANNOT_BE_CHANGED" -> RuleVoluntaryClass2CannotBeChangedError,
      "SERVER_ERROR"                       -> InternalError,
      "SERVICE_UNAVAILABLE"                -> InternalError
    )
    val extra_error = Map {
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError
    }
    error ++ extra_error
  }
}
