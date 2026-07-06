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

package v2.controllers.validators

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.{ResolveIsoDate, ResolveNino, ResolveStringPattern}
import api.models.domain.Nino
import api.models.errors.*
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import v2.models.request.create.CreateMarriageAllowanceRequestData

import java.time.LocalDate

object CreateMarriageAllowanceValidator extends RulesValidator[CreateMarriageAllowanceRequestData] {

  private val nameRegex = "^[A-Za-z0-9 ,.()/&'-]{1,35}$".r
  private val minYear   = 1900
  private val maxYear   = 2100

  override def validateBusinessRules(parsed: CreateMarriageAllowanceRequestData): Validated[Seq[MtdError], CreateMarriageAllowanceRequestData] = {
    import parsed.body.*

    val validatedPartnerSurname = ResolveStringPattern(
      spouseOrCivilPartnerSurname,
      nameRegex,
      PartnerSurnameFormatError
    )

    val validatedPartnerFirstName = ResolveStringPattern(
      spouseOrCivilPartnerFirstName,
      nameRegex,
      PartnerFirstNameFormatError
    )

    val validatedPartnerDateOfBirth = spouseOrCivilPartnerDateOfBirth
      .map(dob => ResolveIsoDate(dob, Some(PartnerDoBFormatError), path = None) andThen validatePartnerDoB)
      .getOrElse(valid)

    combine(
      resolvePartnerNino(spouseOrCivilPartnerNino),
      validatedPartnerSurname,
      validatedPartnerFirstName,
      validatedPartnerDateOfBirth
    ).onSuccess(parsed)
  }

  private def validatePartnerDoB(partnerDoB: LocalDate): Validated[Seq[MtdError], LocalDate] =
    if (partnerDoB.getYear <= maxYear && partnerDoB.getYear >= minYear) Valid(partnerDoB) else Invalid(Seq(PartnerDoBFormatError))

  private def resolvePartnerNino(nino: String) =
    if (ResolveNino.isValid(nino))
      Valid(Nino(nino))
    else
      Invalid(List(PartnerNinoFormatError))

}
