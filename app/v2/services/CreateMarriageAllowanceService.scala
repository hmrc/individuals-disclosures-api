/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.services

import api.controllers.RequestContext
import api.models
import api.models.errors.*
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v2.connectors.CreateMarriageAllowanceConnector
import v2.models.request.create.CreateMarriageAllowanceRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateMarriageAllowanceService @Inject() (connector: CreateMarriageAllowanceConnector) extends BaseService {

  def create(request: CreateMarriageAllowanceRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector
      .create(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap = Map(
    "INVALID_IDVALUE"                 -> NinoFormatError,
    "DECEASED_PARTICIPANT"            -> RuleDeceasedRecipientError,
    "RELATIONSHIP_ALREADY_EXISTS"     -> RuleActiveMarriageAllowanceClaimError,
    "INVALID_IDTYPE"                  -> InternalError,
    "END_DATE_CODE_NOT_FOUND"         -> InternalError,
    "INVALID_CORRELATIONID"           -> InternalError,
    "INVALID_PAYLOAD"                 -> InternalError,
    "NINO_OR_TRN_NOT_FOUND"           -> RuleInvalidRequestError,
    "INVALID_ACTUAL_END_DATE"         -> InternalError,
    "INVALID_PARTICIPANT_END_DATE"    -> InternalError,
    "INVALID_PARTICIPANT_START_DATE"  -> InternalError,
    "INVALID_RELATIONSHIP_CODE"       -> InternalError,
    "PARTICIPANT1_CANNOT_BE_UPDATED"  -> InternalError,
    "PARTICIPANT2_CANNOT_BE_UPDATED"  -> InternalError,
    "CONFIDENCE_CHECK_FAILED"         -> RuleInvalidRequestError.forPersonalDetailsMismatch,
    "CONFIDENCE_CHECK_SURNAME_MISSED" -> InternalError,
    "BAD_GATEWAY"                     -> InternalError,
    "SERVER_ERROR"                    -> InternalError,
    "SERVICE_UNAVAILABLE"             -> InternalError
  )

}
