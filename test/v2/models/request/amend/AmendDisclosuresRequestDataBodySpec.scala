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

package v2.models.request.amend

import play.api.libs.json.{JsError, JsObject, Json}
import support.UnitSpec

class AmendDisclosuresRequestDataBodySpec extends UnitSpec {

  private val json = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "14211123",
      |         "taxYear": "2020-21"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   }
      |}
    """.stripMargin
  )

  private val taxAvoidanceModel = Seq(
    AmendTaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    )
  )

  private val class2NicsModel = AmendClass2Nics(class2VoluntaryContributions = Some(true))

  private val requestBodyModel = AmendDisclosuresRequestBody(
    taxAvoidance = Some(taxAvoidanceModel),
    class2Nics = Some(class2NicsModel)
  )

  "AmendDisclosuresRequestBody" when {
    "read from valid JSON" should {
      "produce the expected AmendDisclosuresRequestBody object" in {
        json.as[AmendDisclosuresRequestBody] shouldBe requestBodyModel
      }
    }

    "read from empty JSON" should {
      "produce an empty AmendDisclosuresRequestBody object" in {
        val emptyJson = JsObject.empty

        emptyJson.as[AmendDisclosuresRequestBody] shouldBe AmendDisclosuresRequestBody.empty
      }
    }

    "read from valid JSON with empty taxAvoidance array and class2Nics object" should {
      "produce an empty AmendDisclosuresRequestBody object" in {
        val json = Json.parse(
          """
            |{
            |   "taxAvoidance": [ ],
            |   "class2Nics": { }
            |}
          """.stripMargin
        )

        json.as[AmendDisclosuresRequestBody] shouldBe AmendDisclosuresRequestBody.empty
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = Json.parse(
          """
            |{
            |   "taxAvoidance": [
            |      {
            |         "srn": true,
            |         "taxYear": "2020-21"
            |      }
            |   ],
            |   "class2Nics": {
            |      "class2VoluntaryContributions": true
            |   }
            |}
          """.stripMargin
        )

        invalidJson.validate[AmendDisclosuresRequestBody] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(requestBodyModel) shouldBe json
      }
    }
  }

}
