/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators

import v1.controllers.requestParsers.validators.validations.{DateFormatValidation, GivenNameValidation, JsonFormatValidation, NinoValidation, SurnameValidation}
import v1.models.errors.{MtdError, PartnerDoBFormatError, PartnerFirstNameFormatError, PartnerNinoFormatError, PartnerSurnameFormatError}
import v1.models.request.marriageAllowance.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRawData}

class CreateMarriageAllowanceValidator extends Validator[CreateMarriageAllowanceRawData] {
  private val parameterFormatValidation = (data: CreateMarriageAllowanceRawData) => {
    List(
      NinoValidation.validate(data.nino)
    )
  }

  private val bodyFormatValidation = (data: CreateMarriageAllowanceRawData) => {
    List(
      JsonFormatValidation.validate[CreateMarriageAllowanceBody](data.body.json)
    )
  }

  private val bodyValueValidation = (data: CreateMarriageAllowanceRawData) => {
    val body = data.body.json.as[CreateMarriageAllowanceBody]

    List(Validator.flattenErrors(List(
      NinoValidation.validate(body.spouseOrCivilPartnerNino, PartnerNinoFormatError),
      SurnameValidation.validate(body.spouseOrCivilPartnerSurname, PartnerSurnameFormatError),
      Validator.validateOptional(body.spouseOrCivilPartnerFirstName)(GivenNameValidation.validate(_, PartnerFirstNameFormatError)),
      Validator.validateOptional(body.spouseOrCivilPartnerDateOfBirth)(DateFormatValidation.validate(_, PartnerDoBFormatError)),
    ))
    )
  }

  private val validationSet = List(parameterFormatValidation, bodyFormatValidation, bodyValueValidation)

  override def validate(data: CreateMarriageAllowanceRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }
}
