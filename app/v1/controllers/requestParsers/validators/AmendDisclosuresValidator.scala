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
import config.AppConfig
import api.models.errors.MtdError
import v1.models.request.amend._

import javax.inject.Inject

class AmendDisclosuresValidator @Inject()(implicit appConfig: AppConfig) extends Validator[AmendDisclosuresRawData] {
  private val validationSet = List(
    parameterFormatValidation,
    parameterRuleValidation,
    bodyFormatValidator,
    bodyValueValidator
  )

  override def validate(data: AmendDisclosuresRawData): List[MtdError] = run(validationSet, data)

  private def parameterFormatValidation: AmendDisclosuresRawData => List[List[MtdError]] =
    data =>
      List(
        NinoValidation.validate(data.nino),
        TaxYearValidation.validate(data.taxYear)
    )

  private def parameterRuleValidation: AmendDisclosuresRawData => List[List[MtdError]] =
    data =>
      List(
        TaxYearNotSupportedValidation.validate(data.taxYear)
    )

  private def bodyFormatValidator: AmendDisclosuresRawData => List[List[MtdError]] =
    data =>
      List(
        JsonFormatValidation.validate[AmendDisclosuresRequestBody](data.body.json)
    )

  private def bodyValueValidator: AmendDisclosuresRawData => List[List[MtdError]] = data => {
    val requestBodyData = data.body.json.as[AmendDisclosuresRequestBody]

    List(
      Validator.flattenErrors(
        List(
          requestBodyData.taxAvoidance
            .map(
              _.zipWithIndex.flatMap(item => validateTaxAvoidance(item._1, item._2))
            )
            .getOrElse(NoValidationErrors)
            .toList,
          requestBodyData.class2Nics.map(validateClass2Nics).getOrElse(NoValidationErrors)
        )
      ))
  }

  private def validateTaxAvoidance(taxAvoidance: AmendTaxAvoidanceItem, arrayIndex: Int): List[MtdError] =
    List(
      SRNValidation
        .validate(taxAvoidance.srn)
        .map(
          _.copy(paths = Some(List(s"/taxAvoidance/$arrayIndex/srn")))
        ),
      TaxYearValidation
        .validate(taxAvoidance.taxYear)
        .map(
          _.copy(paths = Some(List(s"/taxAvoidance/$arrayIndex/taxYear")))
        )
    ).flatten

  private def validateClass2Nics(class2Nics: AmendClass2Nics): List[MtdError] =
    List(
      VoluntaryClass2ValueValidation
        .validateOptional(class2Nics.class2VoluntaryContributions)
        .map(
          _.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions")))
        )
    ).flatten
}
