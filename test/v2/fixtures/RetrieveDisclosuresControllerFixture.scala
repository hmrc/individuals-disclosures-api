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

package v2.fixtures

import play.api.libs.json.{JsValue, Json}

object RetrieveDisclosuresControllerFixture {

  val fullIfsRetrieveDisclosuresResponse: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-21"
      |    },
      |    {
      |      "srn": "34522678",
      |      "taxYear": "2021-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  },
      |  "submittedOn": "2020-07-06T09:37:17.000Z"
      |}
    """.stripMargin
  )

  val fullHipRetrieveDisclosuresResponse: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "SRN": "14211123",
      |      "taxYear": "2020-21"
      |    },
      |    {
      |      "SRN": "34522678",
      |      "taxYear": "2021-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  },
      |  "submittedOn": "2020-07-06T09:37:17.000Z"
      |}
    """.stripMargin
  )

  val mtdResponse: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-21"
      |    },
      |    {
      |      "srn": "34522678",
      |      "taxYear": "2021-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  },
      |  "submittedOn": "2020-07-06T09:37:17.000Z"
      |}
    """.stripMargin
  )

}
