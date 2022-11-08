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
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1.connectors.CreateMarriageAllowanceConnector
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.marriageAllowance.CreateMarriageAllowanceRequest
import v1.support.DownstreamResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateMarriageAllowanceService @Inject()(connector: CreateMarriageAllowanceConnector) extends DownstreamResponseMappingSupport with Logging {

  def create(request: CreateMarriageAllowanceRequest)
            (implicit hc: HeaderCarrier, ec: ExecutionContext,
             logContext: EndpointLogContext,
             correlationId: String): Future[Either[ErrorWrapper, ResponseWrapper[Unit]]] = {

    val result = for {
      downstreamResponseWrapper <- EitherT(connector.create(request)).leftMap(mapDownstreamErrors(ifsErrorMap))
    } yield downstreamResponseWrapper

    result.value
  }

  private def ifsErrorMap = {
    val errors = Map(
      "INVALID_IDVALUE" -> NinoFormatError,
      "DECEASED_PARTICIPANT" -> RuleDeceasedRecipientError,
      "RELATIONSHIP_ALREADY_EXISTS" -> RuleActiveMarriageAllowanceClaimError,
      "INVALID_IDTYPE" -> InternalError,
      "END_DATE_CODE_NOT_FOUND" -> InternalError,
      "INVALID_CORRELATIONID" -> InternalError,
      "INVALID_PAYLOAD" -> InternalError,
      "NINO_OR_TRN_NOT_FOUND" -> RuleInvalidRequestError,
      "INVALID_ACTUAL_END_DATE" -> InternalError,
      "INVALID_PARTICIPANT_END_DATE" -> InternalError,
      "INVALID_PARTICIPANT_START_DATE" -> InternalError,
      "INVALID_RELATIONSHIP_CODE" -> InternalError,
      "PARTICIPANT1_CANNOT_BE_UPDATED" -> InternalError,
      "PARTICIPANT2_CANNOT_BE_UPDATED" -> InternalError,
      "CONFIDENCE_CHECK_FAILED" -> InternalError,
      "CONFIDENCE_CHECK_SURNAME_MISSED" -> InternalError,
      "BAD_GATEWAY" -> InternalError,
      "SERVER_ERROR" -> InternalError,
      "SERVICE_UNAVAILABLE" -> InternalError
    )

    val extra_errors = Map (
      ("INVALID_NINO" -> NinoFormatError )
    )
    errors ++ extra_errors
  }
}
