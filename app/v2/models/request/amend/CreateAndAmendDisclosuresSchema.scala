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

package v2.models.request.amend

import api.controllers.validators.resolvers.ResolveTaxYear
import api.models.domain.TaxYear
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.Valid
import config.AppConfig

sealed trait CreateAndAmendDisclosuresSchema

object CreateAndAmendDisclosuresSchema {

  case object Def1 extends CreateAndAmendDisclosuresSchema
  case object Def2 extends CreateAndAmendDisclosuresSchema

  def schemaFor(taxYearString: String)(implicit appConfig: AppConfig): Validated[Seq[MtdError], CreateAndAmendDisclosuresSchema] =
    ResolveTaxYear(appConfig.minimumPermittedTaxYear, taxYearString, None, None).andThen(schemaForValidated)

  private def schemaForValidated(taxYear: TaxYear): Validated[Seq[MtdError], CreateAndAmendDisclosuresSchema] =
    Valid(if (taxYear.year >= TaxYear.fromMtd("2026-27").year) Def2 else Def1)

}
