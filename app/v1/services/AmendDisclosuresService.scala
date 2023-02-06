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
import api.models.errors._
import api.services.BaseService
import cats.implicits._
import v1.connectors.AmendDisclosuresConnector
import v1.models.request.amend.AmendDisclosuresRequest

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class AmendDisclosuresService @Inject()(connector: AmendDisclosuresConnector) extends BaseService {

  def amendDisclosures(request: AmendDisclosuresRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[AmendPensionChargesOutcome] = {

    connector
      .amendDisclosures(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private def downstreamErrorMap = {
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
