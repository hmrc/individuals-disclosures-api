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

package v2.models.request.amend

import config.MockAppConfig
import play.api.Configuration
import play.api.libs.json._
import support.UnitSpec

class AmendTaxAvoidanceItemSpec extends UnitSpec with MockAppConfig {

  private def json(srnUpperCase: Boolean = false): JsValue = {
    val srn = if (srnUpperCase) "SRN" else "srn"
    Json.parse(
      s"""
        |{
        |   "$srn": "14211123",
        |   "taxYear": "2020-21"
        |}
    """.stripMargin
    )
  }

  private val model: AmendTaxAvoidanceItem = AmendTaxAvoidanceItem(
    srn = "14211123",
    taxYear = "2020-21"
  )

  "AmendTaxAvoidanceItem" when {
    "read from valid JSON" should {
      "produce the expected AmendTaxAvoidanceItem object" in {
        json().as[AmendTaxAvoidanceItem] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = JsObject.empty

        invalidJson.validate[AmendTaxAvoidanceItem] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject when using HIP format downstream" in {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> true))
        val isHipEnabled = true
        Json.toJson(model) shouldBe json(isHipEnabled)
      }

      "produce the expected JsObject when using IFS format downstream" in {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> false))
        val isHipEnabled = false
        Json.toJson(model) shouldBe json(isHipEnabled)
      }
    }
  }

}
