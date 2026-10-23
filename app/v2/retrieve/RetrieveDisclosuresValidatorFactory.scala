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

package v2.retrieve

import api.controllers.validators.Validator
import cats.data.Validated.{Invalid, Valid}
import config.AppConfig
import v2.retrieve.RetrieveDisclosuresSchema.{Def1, Def2}
import v2.retrieve.def1.Def1_RetrieveDisclosuresValidator
import v2.retrieve.def2.Def2_RetrieveDisclosuresValidator
import v2.retrieve.model.request.RetrieveDisclosuresRequestData

import javax.inject.{Inject, Singleton}

@Singleton
class RetrieveDisclosuresValidatorFactory @Inject() (implicit appConfig: AppConfig) {

  def validator(nino: String, taxYear: String): Validator[RetrieveDisclosuresRequestData] = {

    val schema = RetrieveDisclosuresSchema.schemaFor(taxYear)

    schema match
      case Valid(Def1)     => Def1_RetrieveDisclosuresValidator(nino, taxYear)
      case Valid(Def2)     => Def2_RetrieveDisclosuresValidator(nino, taxYear)
      case Invalid(errors) => Validator.returningErrors(errors)

  }

}
