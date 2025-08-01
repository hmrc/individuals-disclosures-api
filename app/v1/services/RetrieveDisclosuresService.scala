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
import api.models.errors
import api.models.errors.*
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v1.connectors.RetrieveDisclosuresConnector
import v1.models.request.retrieve.RetrieveDisclosuresRequestData
import v1.models.response.retrieveDisclosures.RetrieveDisclosuresResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveDisclosuresService @Inject() (connector: RetrieveDisclosuresConnector) extends BaseService {

  def retrieve(request: RetrieveDisclosuresRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[RetrieveDisclosuresResponse]] = {

    connector
      .retrieve(request)
      .map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))

  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val ifsErrors = Map(
      "INVALID_NINO"          -> NinoFormatError,
      "INVALID_TAX_YEAR"      -> TaxYearFormatError,
      "INVALID_CORRELATIONID" -> errors.InternalError,
      "NO_DATA_FOUND"         -> NotFoundError,
      "SERVER_ERROR"          -> errors.InternalError,
      "SERVICE_UNAVAILABLE"   -> errors.InternalError
    )

    val hipErrors = Map(
      "1215" -> NinoFormatError,
      "1117" -> TaxYearFormatError,
      "5010" -> NotFoundError
    )

    ifsErrors ++ hipErrors
  }

}
