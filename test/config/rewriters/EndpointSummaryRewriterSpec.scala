/*
 * Copyright 2025 HM Revenue & Customs
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

package config.rewriters

import config.MockAppConfig
import support.UnitSpec

class EndpointSummaryRewriterSpec extends UnitSpec with MockAppConfig {
  
  val rewriter = new EndpointSummaryRewriter(mockAppConfig)

  "check and rewrite for the endpoint yaml file" when {
    val (check, rewrite) = rewriter.rewriteEndpointSummary.asTuple
    "check() is given marriageAllowance.yaml with the endpoint API docs disabled (assuming in production)" should {
      "indicate rewrite needed" in {
        MockedAppConfig.endpointReleasedInProduction("2.0", "marriageAllowance") returns false
        val result = check("2.0", "marriageAllowance.yaml")
        result shouldBe true
      }
    }
    "check() is given any other combination" should {
      "indicate rewrite not needed" in {
        MockedAppConfig.endpointReleasedInProduction("2.0", "marriageAllowance") returns true
        val result = check("2.0", "marriageAllowance.yaml")
        result shouldBe false
      }
    }
    "the summary already contains [test only]" should {
      "return the summary unchanged" in {
        val summary = """summary: "[tesT oNLy] Create Marriage Allowance""""
        val result  = rewrite("", "", summary)
        result shouldBe summary
      }
    }
    "the yaml summary is ready to be rewritten" should {
      "return the rewritten summary, in quotes due to the '[' special character" in {
        val result = rewrite("", "", "summary: Create Marriage Allowance")
        result shouldBe """summary: "Create Marriage Allowance [test only]""""
      }
      "return the rewritten summary preserving indentation" in {
        val result = rewrite("", "", "  summary: Create Marriage Allowance")
        result shouldBe """  summary: "Create Marriage Allowance [test only]""""
      }

      "return the rewritten summary when it contains parentheses" in {
        val result = rewrite("", "", "summary: Create Marriage Allowance (non-PPD)")
        result shouldBe """summary: "Create Marriage Allowance (non-PPD) [test only]""""
      }

      "return the rewritten summary when it contains square brackets" in {
        val result = rewrite("", "", "summary: Create Marriage Allowance [non-PPD]")
        result shouldBe """summary: "Create Marriage Allowance [non-PPD] [test only]""""
      }
    }

    "the yaml summary is already in quotes" should {
      "return the rewritten summary" in {
        val result = rewrite("", "", """summary: "Create Marriage Allowance"""")
        result shouldBe """summary: "Create Marriage Allowance [test only]""""
      }
    }
    "the yaml is not for a single endpoint" should {
      "return the yaml unchanged" in {
        val yaml = """
                     |summary: "Create and Amend employment expenses"
                     |summary: "Create and Amend employment expenses"""".stripMargin
        val result = rewrite("", "", yaml)
        result shouldBe yaml
      }
    }
  }
}
