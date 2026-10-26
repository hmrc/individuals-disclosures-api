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

package v2.models.request.amend

import api.models.errors.{RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import cats.data.Validated.{Invalid, Valid}
import config.MockAppConfig
import support.UnitSpec
import v2.models.request.amend.CreateAndAmendDisclosuresSchema.{Def1, Def2, schemaFor}

class CreateAndAmendDisclosuresSchemaSpec extends UnitSpec with MockAppConfig {

  "schemaFor" should {
    "select Def1 before 2026-27, including the minimum permitted tax year" in {
      MockedAppConfig.minimumPermittedTaxYear.returns(2020).twice()

      schemaFor("2019-20") shouldBe Valid(Def1)
      schemaFor("2025-26") shouldBe Valid(Def1)
    }

    "select Def2 from 2026-27 onwards" in {
      MockedAppConfig.minimumPermittedTaxYear.returns(2020).twice()

      schemaFor("2026-27") shouldBe Valid(Def2)
      schemaFor("2027-28") shouldBe Valid(Def2)
    }

    "reject tax years below the configured minimum" in {
      MockedAppConfig.minimumPermittedTaxYear.returns(2020)

      schemaFor("2018-19") shouldBe Invalid(List(RuleTaxYearNotSupportedError))
    }

    "reject incorrectly formatted tax years" in {
      MockedAppConfig.minimumPermittedTaxYear.returns(2020)

      schemaFor("2025") shouldBe Invalid(List(TaxYearFormatError))
    }

    "reject invalid tax year ranges" in {
      MockedAppConfig.minimumPermittedTaxYear.returns(2020)

      schemaFor("2025-27") shouldBe Invalid(List(RuleTaxYearRangeInvalidError))
    }
  }

}
