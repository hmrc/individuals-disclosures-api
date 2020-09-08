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

package v1.models.response.retrieveDisclosures

import play.api.libs.json.{JsError, JsObject, Json}
import support.UnitSpec
import v1.models.request.disclosures.Class2Nics

class RetrieveDisclosuresResponseSpec extends UnitSpec {

  private val json = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-21"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  },
      |  "submittedOn": "2020-07-06T09:37:17Z"
      |}
    """.stripMargin
  )

  private val taxAvoidanceItemModel = Seq(
    TaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    )
  )

  private val class2Nics = Class2Nics(true)

  private val responseModel = RetrieveDisclosuresResponse(
    Some(taxAvoidanceItemModel), Some(class2Nics), Some("2020-07-06T09:37:17Z")
  )

  "RetrieveDisclosuresResponse" when {
    "read from valid JSON" should {
      "produce the expected RetrieveDisclosuresResponse object" in {
        json.as[RetrieveDisclosuresResponse] shouldBe responseModel
      }
    }

    "read from valid JSON with empty taxAvoidance array" should {
      "produce an empty RetrieveDisclosuresResponse object" in {
        val json = Json.parse(
          """
            |{
            |   "taxAvoidance": [ ]
            |}
          """.stripMargin
        )

        json.as[RetrieveDisclosuresResponse] shouldBe RetrieveDisclosuresResponse.empty
      }
    }

    "read from empty JSON" should {
      "produce an empty RetrieveDisclosuresResponse object" in {
        val emptyJson = JsObject.empty

        emptyJson.as[RetrieveDisclosuresResponse] shouldBe RetrieveDisclosuresResponse.empty
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = Json.parse(
          """
            |{
            |  "taxAvoidance": [
            |    {
            |      "srn": true,
            |      "taxYear": "2020-21"
            |    }
            |  ],
            |    "class2Nics": {
            |     "class2VoluntaryContributions": true
            |  },
            |  "submittedOn": "2020-07-06T09:37:17Z"
            |}
          """.stripMargin
        )
        invalidJson.validate[RetrieveDisclosuresResponse] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(responseModel) shouldBe json
      }
    }
  }
}
