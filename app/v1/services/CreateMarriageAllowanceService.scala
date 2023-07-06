/*
 * Copyright 2023 HM Revenue & Customs
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

import api.controllers.RequestContext
import api.models
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits._
import v1.connectors.CreateMarriageAllowanceConnector
import v1.models.request.create.CreateMarriageAllowanceRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateMarriageAllowanceService @Inject() (connector: CreateMarriageAllowanceConnector) extends BaseService {

  def create(request: CreateMarriageAllowanceRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector
      .create(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap = {
    val errors = Map(
      "INVALID_IDVALUE"                 -> NinoFormatError,
      "DECEASED_PARTICIPANT"            -> RuleDeceasedRecipientError,
      "RELATIONSHIP_ALREADY_EXISTS"     -> RuleActiveMarriageAllowanceClaimError,
      "INVALID_IDTYPE"                  -> models.errors.InternalError,
      "END_DATE_CODE_NOT_FOUND"         -> models.errors.InternalError,
      "INVALID_CORRELATIONID"           -> models.errors.InternalError,
      "INVALID_PAYLOAD"                 -> models.errors.InternalError,
      "NINO_OR_TRN_NOT_FOUND"           -> RuleInvalidRequestError,
      "INVALID_ACTUAL_END_DATE"         -> models.errors.InternalError,
      "INVALID_PARTICIPANT_END_DATE"    -> models.errors.InternalError,
      "INVALID_PARTICIPANT_START_DATE"  -> models.errors.InternalError,
      "INVALID_RELATIONSHIP_CODE"       -> models.errors.InternalError,
      "PARTICIPANT1_CANNOT_BE_UPDATED"  -> models.errors.InternalError,
      "PARTICIPANT2_CANNOT_BE_UPDATED"  -> models.errors.InternalError,
      "CONFIDENCE_CHECK_FAILED"         -> models.errors.InternalError,
      "CONFIDENCE_CHECK_SURNAME_MISSED" -> models.errors.InternalError,
      "BAD_GATEWAY"                     -> models.errors.InternalError,
      "SERVER_ERROR"                    -> models.errors.InternalError,
      "SERVICE_UNAVAILABLE"             -> models.errors.InternalError
    )

    val extra_errors = Map(
      "INVALID_NINO" -> NinoFormatError
    )
    errors ++ extra_errors
  }

}
