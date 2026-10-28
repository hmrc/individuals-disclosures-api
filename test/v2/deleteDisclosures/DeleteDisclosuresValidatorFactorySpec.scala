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

package v2.deleteDisclosures

import api.controllers.validators.{AlwaysErrorsValidator, Validator}
import config.MockAppConfig
import support.UnitSpec
import v2.deleteDisclosures.def1.Def1_DeleteDisclosuresValidator
import v2.deleteDisclosures.model.request.DeleteDisclosuresRequestData

class DeleteDisclosuresValidatorFactorySpec extends UnitSpec with MockAppConfig {

  private def validatorFor(taxYear: String): Validator[DeleteDisclosuresRequestData] =
    new DeleteDisclosuresValidatorFactory().validator(nino = "ignoredNino", taxYear = taxYear)

  private trait Test {
    MockedAppConfig.minimumPermittedTaxYear.returns(2022).anyNumberOfTimes()
  }

  "DeleteDisclosuresValidatorFactory" when {
    "given a request corresponding to a Def1 schema" should {
      "return a Def1 validator" in new Test {
        validatorFor("2026-27") shouldBe a[Def1_DeleteDisclosuresValidator]
      }
    }

    "given a request where no valid schema could be determined" should {
      "return a validator returning the errors" in new Test {
        validatorFor("BAD_TAX_YEAR") shouldBe an[AlwaysErrorsValidator]
      }
    }
  }

}
