/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import javax.inject.Inject
import v1.controllers.requestParsers.validators.validations._
import v1.models.errors.MtdError
import v1.models.request.disclosures.{AmendDisclosuresRawData, AmendDisclosuresRequestBody, AmendTaxAvoidance}

class AmendDisclosuresValidator @Inject()(implicit appConfig: AppConfig) extends Validator[AmendDisclosuresRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyValueValidator)

  override def validate(data: AmendDisclosuresRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: AmendDisclosuresRawData => List[List[MtdError]] = (data: AmendDisclosuresRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidator: AmendDisclosuresRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendDisclosuresRequestBody](data.body.json)
    )
  }

  private def bodyValueValidator: AmendDisclosuresRawData => List[List[MtdError]] = { data =>

    val requestBodyData = data.body.json.as[AmendDisclosuresRequestBody]

    List(flattenErrors(
      List(
        requestBodyData.taxAvoidance.map(_.zipWithIndex.flatMap {
          case (data, index) => validateTaxAvoidance(data, index)
        }).getOrElse(NoValidationErrors).toList
      )
    ))
  }

  private def validateTaxAvoidance(taxAvoidance: AmendTaxAvoidance, arrayIndex: Int): List[MtdError] = {
    List(
      SRNValidation.validate(taxAvoidance.srn).map(
        _.copy(paths = Some(Seq(s"/taxAvoidance/$arrayIndex/srn")))
      ),
      TaxYearValidation.validate(taxAvoidance.taxYear).map(
        _.copy(paths = Some(Seq(s"/taxAvoidance/$arrayIndex/taxYear")))
      ),
      TaxYearNotSupportedValidation.validate(taxAvoidance.taxYear).map(
        _.copy(paths = Some(Seq(s"/taxAvoidance/$arrayIndex/taxYear")))
      )
    ).flatten
  }
}