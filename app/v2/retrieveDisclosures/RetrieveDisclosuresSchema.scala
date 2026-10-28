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

package v2.retrieveDisclosures

import api.controllers.validators.resolvers.ResolveTaxYear
import api.models.domain.TaxYear
import api.models.errors.MtdError
import api.schema.DownstreamReadable
import cats.data.Validated
import cats.data.Validated.Valid
import config.AppConfig
import play.api.libs.json.Reads
import v2.retrieveDisclosures.def1.model.response.Def1_RetrieveDisclosuresResponse
import v2.retrieveDisclosures.def2.model.response.Def2_RetrieveDisclosuresResponse
import v2.retrieveDisclosures.model.response.RetrieveDisclosuresResponse

import scala.math.Ordered.orderingToOrdered

sealed trait RetrieveDisclosuresSchema extends DownstreamReadable[RetrieveDisclosuresResponse]

object RetrieveDisclosuresSchema {

  case object Def1 extends RetrieveDisclosuresSchema {
    type DownstreamResp = Def1_RetrieveDisclosuresResponse
    val connectorReads: Reads[DownstreamResp] = Def1_RetrieveDisclosuresResponse.reads
  }

  case object Def2 extends RetrieveDisclosuresSchema {
    type DownstreamResp = Def2_RetrieveDisclosuresResponse
    val connectorReads: Reads[DownstreamResp] = Def2_RetrieveDisclosuresResponse.reads
  }

  def schemaFor(taxYearString: String)(implicit appConfig: AppConfig): Validated[Seq[MtdError], RetrieveDisclosuresSchema] =
    ResolveTaxYear(minimumTaxYear = appConfig.minimumPermittedTaxYear, value = taxYearString, error = None, path = None) andThen schemaFor

  def schemaFor(taxYear: TaxYear): Validated[Seq[MtdError], RetrieveDisclosuresSchema] =
    if (taxYear >= TaxYear.fromMtd("2026-27")) Valid(Def2) else Valid(Def1)

}
