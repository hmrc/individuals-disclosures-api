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

import api.models.domain.{TaxYear, TaxYearPropertyCheckSupport}
import api.models.errors.*
import cats.data.Validated.{Invalid, Valid}
import config.MockAppConfig
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import support.UnitSpec
import v2.deleteDisclosures.DeleteDisclosuresSchema.*

class DeleteDisclosuresSchemaSpec extends UnitSpec with MockAppConfig with ScalaCheckDrivenPropertyChecks with TaxYearPropertyCheckSupport {

  private trait Test {
    MockedAppConfig.minimumPermittedTaxYear.returns(2022).anyNumberOfTimes()
  }

  "schema lookup" when {
    "a valid tax year is supplied" should {
      "use Def1 schema for tax years 2021-22 onwards" in new Test {
        forTaxYearsFrom(TaxYear.fromMtd("2021-22")) { taxYear =>
          schemaFor(taxYear.asMtd) shouldBe Valid(Def1)
        }
      }
    }

    "handle errors" when {
      "an invalid tax year is supplied" should {
        "disallow tax years prior to 2021-22 and return RuleTaxYearNotSupportedError" in new Test {
          forTaxYearsBefore(TaxYear.fromMtd("2021-22")) { taxYear =>
            schemaFor(taxYear.asMtd) shouldBe Invalid(Seq(RuleTaxYearNotSupportedError))
          }
        }
      }

      "the tax year format is invalid" should {
        "return TaxYearFormatError" in new Test {
          schemaFor("2026") shouldBe Invalid(Seq(TaxYearFormatError))
        }
      }

      "the tax year range is invalid" should {
        "return RuleTaxYearRangeInvalidError" in new Test {
          schemaFor("2026-28") shouldBe Invalid(Seq(RuleTaxYearRangeInvalidError))
        }
      }
    }
  }

}
