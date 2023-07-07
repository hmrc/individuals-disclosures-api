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

package v1.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.Validator
import api.controllers.requestParsers.validators.validations._
import api.models.errors._
import v1.models.request.create.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRawData}

class CreateMarriageAllowanceValidator extends Validator[CreateMarriageAllowanceRawData] {
  private val validationSet = List(parameterFormatValidation, bodyFormatValidation, bodyValueValidation)

  override def validate(data: CreateMarriageAllowanceRawData): List[MtdError] = run(validationSet, data)

  private def parameterFormatValidation: CreateMarriageAllowanceRawData => List[List[MtdError]] =
    data =>
      List(
        NinoValidation.validate(data.nino)
      )

  private def bodyFormatValidation: CreateMarriageAllowanceRawData => List[List[MtdError]] =
    data =>
      List(
        JsonFormatValidation.validate[CreateMarriageAllowanceBody](data.body.json)
      )

  private def bodyValueValidation: CreateMarriageAllowanceRawData => List[List[MtdError]] = data => {
    val body = data.body.json.as[CreateMarriageAllowanceBody]
    import body._

    List(
      Validator.flattenErrors(
        List(
          NinoValidation.validate(spouseOrCivilPartnerNino, PartnerNinoFormatError),
          SurnameValidation.validate(spouseOrCivilPartnerSurname, PartnerSurnameFormatError),
          Validator.validateOptional(spouseOrCivilPartnerFirstName)(GivenNameValidation.validate(_, PartnerFirstNameFormatError)),
          Validator.validateOptional(spouseOrCivilPartnerDateOfBirth)(DateFormatValidation.validate(_, PartnerDoBFormatError))
        )
      ))
  }

}
