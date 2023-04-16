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
import cats.implicits.toBifunctorOps
import v1.connectors.DeleteDisclosuresConnector

import javax.inject.{ Inject, Singleton }
import v1.models.request.delete.DeleteDisclosuresRequest

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class DeleteDisclosuresService @Inject()(connector: DeleteDisclosuresConnector) extends BaseService {

  def delete(request: DeleteDisclosuresRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[DeleteDisclosuresOutcome] = {
    connector
      .deleteDisclosures(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID"          -> NinoFormatError,
    "INVALID_TAX_YEAR"                   -> TaxYearFormatError,
    "INVALID_CORRELATIONID"              -> InternalError,
    "NO_DATA_FOUND"                      -> NotFoundError,
    "VOLUNTARY_CLASS2_CANNOT_BE_CHANGED" -> RuleVoluntaryClass2CannotBeChangedError,
    "SERVER_ERROR"                       -> InternalError,
    "SERVICE_UNAVAILABLE"                -> InternalError
  )
}