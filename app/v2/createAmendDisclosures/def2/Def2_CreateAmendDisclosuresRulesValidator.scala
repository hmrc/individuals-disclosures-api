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

package v2.createAmendDisclosures.def2

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.{ResolveStringPattern, ResolveTaxYear}
import api.models.errors.{MtdError, RuleVoluntaryClass2ValueInvalidError, SRNFormatError}
import cats.data.Validated
import cats.implicits.toFoldableOps
import v2.createAmendDisclosures.def2.model.request.*

import scala.util.matching.Regex

object Def2_CreateAmendDisclosuresRulesValidator extends RulesValidator[Def2_CreateAmendDisclosuresRequestData] {

  private val srnRegex: Regex = "^[0-9]{8}$".r

  override def validateBusinessRules(
      parsed: Def2_CreateAmendDisclosuresRequestData): Validated[Seq[MtdError], Def2_CreateAmendDisclosuresRequestData] = {
    import parsed.body

    combine(
      validateTaxAvoidance(body.taxAvoidance),
      validateClass2Nics(body.class2Nics)
    ).onSuccess(parsed)
  }

  private def validateTaxAvoidance(taxAvoidance: Option[Seq[TaxAvoidanceItem]]): Validated[Seq[MtdError], Unit] =
    taxAvoidance
      .getOrElse(Seq.empty)
      .zipWithIndex
      .traverse_ { case (taxAvoidanceItem, index) =>
        val basePath: String = s"/taxAvoidance/$index"

        combine(
          ResolveStringPattern(taxAvoidanceItem.srn, srnRegex, SRNFormatError.withPath((s"$basePath/srn"))),
          ResolveTaxYear(taxAvoidanceItem.taxYear, error = None, path = None).leftMap(_.map(_.withPath(s"$basePath/taxYear")))
        )
      }

  private def validateClass2Nics(class2Nics: Option[Class2Nics]): Validated[Seq[MtdError], Unit] =
    Validated.cond(
      class2Nics.forall(_.class2VoluntaryContributions.contains(true)),
      (),
      Seq(RuleVoluntaryClass2ValueInvalidError.withPath("/class2Nics/class2VoluntaryContributions"))
    )

}
