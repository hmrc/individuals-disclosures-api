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

import api.models.errors._
import config.AppConfig
import mocks.MockAppConfig
import support.UnitSpec
import v1.models.request.retrieve.RetrieveDisclosuresRawData

class RetrieveDisclosuresValidatorSpec extends UnitSpec with MockAppConfig {
  private val validNino    = "AA123456A"
  private val validTaxYear = "2021-22"

  class Test extends MockAppConfig {
    implicit val appConfig: AppConfig = mockAppConfig
    val validator                     = new RetrieveDisclosuresValidator()

    MockAppConfig.minimumPermittedTaxYear
      .returns(2022)
      .anyNumberOfTimes()
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(RetrieveDisclosuresRawData(validNino, validTaxYear)) shouldBe List.empty[MtdError]
      }
    }

    // parameter format error scenarios
    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(RetrieveDisclosuresRawData("A12344A", validTaxYear)) shouldBe List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(RetrieveDisclosuresRawData(validNino, "20178")) shouldBe List(TaxYearFormatError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors (path parameters)" in new Test {
        validator.validate(RetrieveDisclosuresRawData("A12344A", "20178")) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }

    // parameter rule error scenarios
    "return RuleTaxYearNotSupportedError error" when {
      "an unsupported tax year is supplied" in new Test {
        validator.validate(RetrieveDisclosuresRawData(validNino, "2020-21")) shouldBe List(RuleTaxYearNotSupportedError)
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year range is supplied" in new Test {
        validator.validate(RetrieveDisclosuresRawData(validNino, "2019-21")) shouldBe List(RuleTaxYearRangeInvalidError)
      }
    }
  }
}
