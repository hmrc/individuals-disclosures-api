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
import mocks.MockAppConfig
import support.UnitSpec
import v1.models.errors._
import v1.models.request.DeleteRetrieveRawData
import v1.models.request.disclosures.AmendDisclosuresRawData

class DeleteRetrieveValidatorSpec extends UnitSpec with MockAppConfig {

  private val validNino = "AA123456A"
  private val validTaxYear = "2021-22"

  implicit val appConfig: AppConfig = mockAppConfig

  val validator = new DeleteRetrieveValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(DeleteRetrieveRawData(validNino, validTaxYear)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {

        validator.validate(DeleteRetrieveRawData("A12344A", validTaxYear)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {

        validator.validate(DeleteRetrieveRawData(validNino, "20178")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return a TaxYearNotSupported error" when {
      "an unsupported date is submitted in the request body" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(DeleteRetrieveRawData(validNino, "2019-20")) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {

        validator.validate(DeleteRetrieveRawData("A12344A", "20178")) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}