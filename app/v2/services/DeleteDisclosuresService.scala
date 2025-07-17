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

package v2.services

import api.controllers.RequestContext
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.toBifunctorOps
import v2.connectors.DeleteDisclosuresConnector
import v2.models.request.delete.DeleteDisclosuresRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteDisclosuresService @Inject() (connector: DeleteDisclosuresConnector) extends BaseService {

  def delete(request: DeleteDisclosuresRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {
    connector
      .deleteDisclosures(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val ifsErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID"          -> NinoFormatError,
      "INVALID_TAX_YEAR"                   -> TaxYearFormatError,
      "INVALID_CORRELATIONID"              -> InternalError,
      "NO_DATA_FOUND"                      -> NotFoundError,
      "OUTSIDE_AMENDMENT_WINDOW"           -> RuleOutsideAmendmentWindowError,
      "SERVER_ERROR"                       -> InternalError,
      "SERVICE_UNAVAILABLE"                -> InternalError
    )
    val hipErrors = Map(
      "1215" -> NinoFormatError,
      "1117" -> TaxYearFormatError,
      "5010" -> NotFoundError,
      "4200" -> RuleOutsideAmendmentWindowError,
      "5000" -> RuleTaxYearNotSupportedError
    )
    ifsErrors ++ hipErrors
  }

}
