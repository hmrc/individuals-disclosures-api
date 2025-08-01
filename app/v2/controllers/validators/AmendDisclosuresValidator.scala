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

package v2.controllers.validators

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.ResolveTaxYear
import api.models.errors.{MtdError, RuleVoluntaryClass2ValueInvalidError, SRNFormatError}
import cats.data.Validated
import cats.data.Validated.Invalid
import v2.models.request.amend.{AmendClass2Nics, AmendDisclosuresRequestData, AmendTaxAvoidanceItem}

object AmendDisclosuresValidator extends RulesValidator[AmendDisclosuresRequestData] {

  private val SRNRegex = "^[0-9]{8}$"

  override def validateBusinessRules(parsed: AmendDisclosuresRequestData): Validated[Seq[MtdError], AmendDisclosuresRequestData] = {
    import parsed.body

    val validatedTaxAvoidance = body.taxAvoidance match {
      case Some(taxAvoidanceItems) =>
        combine(taxAvoidanceItems.zipWithIndex.map { case (item, idx) => validateTaxAvoidance(item, idx) }*)
      case None => valid
    }

    val validatedClass2Nics = body.class2Nics.map(validateClass2Nics).getOrElse(valid)

    combine(
      validatedTaxAvoidance,
      validatedClass2Nics
    ).onSuccess(parsed)
  }

  private def validateTaxAvoidance(taxAvoidance: AmendTaxAvoidanceItem, arrayIndex: Int): Validated[Seq[MtdError], Unit] = {
    val validatedSRN =
      if (taxAvoidance.srn.matches(SRNRegex)) valid else Invalid(List(SRNFormatError.copy(paths = Some(List(s"/taxAvoidance/$arrayIndex/srn")))))

    val validatedTaxYear = ResolveTaxYear(taxAvoidance.taxYear, error = None, path = None) match {
      case Invalid(List(error: MtdError)) => Invalid(List(error.copy(paths = Some(List(s"/taxAvoidance/$arrayIndex/taxYear")))))
      case other                          => other
    }

    combine(
      validatedSRN,
      validatedTaxYear
    )
  }

  private def validateClass2Nics(class2Nics: AmendClass2Nics): Validated[Seq[MtdError], Unit] = {
    class2Nics.class2VoluntaryContributions match {
      case Some(true) => valid
      case _          => Invalid(List(RuleVoluntaryClass2ValueInvalidError.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions")))))
    }
  }

}
