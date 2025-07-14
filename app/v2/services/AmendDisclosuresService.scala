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

package v2.services

import api.controllers.RequestContext
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits._
import v2.connectors.AmendDisclosuresConnector
import v2.models.request.amend.AmendDisclosuresRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendDisclosuresService @Inject() (connector: AmendDisclosuresConnector) extends BaseService {

  def amendDisclosures(request: AmendDisclosuresRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector
      .amendDisclosures(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private def downstreamErrorMap: Map[String, MtdError] = {
    val ifsErrors = Map(
      "INVALID_NINO"                       -> NinoFormatError,
      "INVALID_TAX_YEAR"                   -> TaxYearFormatError,
      "INVALID_CORRELATIONID"              -> InternalError,
      "INVALID_PAYLOAD"                    -> InternalError,
      "INCOME_SOURCE_NOT_FOUND"            -> NotFoundError,
      "VOLUNTARY_CLASS2_CANNOT_BE_CHANGED" -> RuleVoluntaryClass2CannotBeChangedError,
      "OUTSIDE_AMENDMENT_WINDOW"           -> RuleOutsideAmendmentWindowError,
      "SERVER_ERROR"                       -> InternalError,
      "SERVICE_UNAVAILABLE"                -> InternalError
    )

    val hipErrors = Map(
      "1000" -> InternalError,
      "1117" -> TaxYearFormatError,
      "1215" -> NinoFormatError,
      "1216" -> InternalError,
      "4200" -> RuleOutsideAmendmentWindowError,
      "5000" -> RuleTaxYearNotSupportedError,
      "5003" -> NotFoundError,
      "5004" -> RuleVoluntaryClass2CannotBeChangedError
    )

    ifsErrors ++ hipErrors
  }

}
