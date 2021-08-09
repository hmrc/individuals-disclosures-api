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
import v1.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateMarriageAllowanceService @Inject()(connector: CreateMarriageAllowanceConnector) extends DesResponseMappingSupport with Logging {

  def create(request: CreateMarriageAllowanceRequest)
                      (implicit hc: HeaderCarrier,ec: ExecutionContext,
                       logContext: EndpointLogContext,
                       correlationId: String): Future[Either[ErrorWrapper, ResponseWrapper[Unit]]] = {

    val result = for {
      desResponseWrapper <- EitherT(connector.create(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper

    result.value
  }

  private def desErrorMap =
    Map(
      "INVALID_IDVALUE" -> NinoFormatError,
      "DECEASED_PARTICIPANT" -> RuleDeceasedRecipientError,
      "RELATIONSHIP_ALREADY_EXISTS" -> RuleActiveMarriageAllowanceClaimError,
      "INVALID_IDTYPE" -> DownstreamError,
      "END_DATE_CODE_NOT_FOUND" -> DownstreamError,
      "INVALID_CORRELATIONID" -> DownstreamError,
      "INVALID_PAYLOAD" -> DownstreamError,
      "NINO_OR_TRN_NOT_FOUND" -> DownstreamError,
      "INVALID_ACTUAL_END_DATE" -> DownstreamError,
      "INVALID_PARTICIPANT_END_DATE" -> DownstreamError,
      "INVALID_PARTICIPANT_START_DATE" -> DownstreamError,
      "INVALID_RELATIONSHIP_CODE" -> DownstreamError,
      "PARTICIPANT1_CANNOT_BE_UPDATED" -> DownstreamError,
      "PARTICIPANT2_CANNOT_BE_UPDATED" -> DownstreamError,
      "CONFIDENCE_CHECK_FAILED" -> DownstreamError,
      "CONFIDENCE_CHECK_SURNAME_MISSED" -> DownstreamError,
      "BAD_GATEWAY" -> DownstreamError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}
