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

package v2.retrieve.def2

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveNino, ResolverSupport}
import api.models.domain.TaxYear
import api.models.errors.MtdError
import cats.data.Validated
import v2.retrieve.def2.model.request.Def2_RetrieveDisclosuresRequestData
import v2.retrieve.model.request.RetrieveDisclosuresRequestData

import javax.inject.Inject

class Def2_RetrieveDisclosuresValidator @Inject() (
    nino: String,
    taxYear: String
) extends Validator[RetrieveDisclosuresRequestData]
    with ResolverSupport {

  override def validate: Validated[Seq[MtdError], RetrieveDisclosuresRequestData] =
    ResolveNino(nino).map { validNino =>
      Def2_RetrieveDisclosuresRequestData(
        nino = validNino,
        taxYear = TaxYear.fromMtd(taxYear)
      )
    }

}
