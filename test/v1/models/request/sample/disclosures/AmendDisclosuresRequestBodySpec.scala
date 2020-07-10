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

package v1.models.request.sample.disclosures

import play.api.libs.json.{JsError, JsObject, Json}
import support.UnitSpec
import v1.models.request.disclosures.{AmendDisclosuresRequestBody, AmendTaxAvoidance}

class AmendDisclosuresRequestBodySpec extends UnitSpec {

  val model: AmendDisclosuresRequestBody = AmendDisclosuresRequestBody(Some(Seq(AmendTaxAvoidance("123","12-12"))))
  val json = Json.parse(
    """
      |{
      |   "taxAvoidance": [{"srn":"123","taxYear":"12-12"}]
      |}
        """.stripMargin
  )
  
  "DisclosuresRequestBodyModel" when {
    "read from valid JSON" should {
      "produce the expected DisclosuresRequestBody object" in {
        json.as[AmendDisclosuresRequestBody] shouldBe model
      }
    }

    "read from empty JSON" should {
      "produce an empty DisclosuresRequestBody object" in {
        val emptyJson = JsObject.empty

        emptyJson.as[AmendDisclosuresRequestBody] shouldBe AmendDisclosuresRequestBody.empty
      }
    }

    "read from valid JSON with empty foreignDividend and dividendIncomeReceivedWhilstAbroad arrays" should {
      "produce an empty DisclosuresRequestBody object" in {
        val json = Json.parse(
          """
            |{
            |   "foreignDividend": [ ],
            |   "dividendIncomeReceivedWhilstAbroad": [ ]
            |}
        """.stripMargin
        )

        json.as[AmendDisclosuresRequestBody] shouldBe AmendDisclosuresRequestBody.empty
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val error = Json.parse(
          """
            |{"taxAvoidance":"b"}
            |""".stripMargin
        )

        error.validate[AmendDisclosuresRequestBody] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(model) shouldBe json
      }
    }
  }
}
