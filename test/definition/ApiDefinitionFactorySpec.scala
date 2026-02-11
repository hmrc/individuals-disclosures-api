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

package definition

import cats.implicits.catsSyntaxValidatedId
import config.{MockAppConfig}
import config.Deprecation.NotDeprecated
import definition.APIStatus.{ALPHA, BETA}
import mocks.MockHttpClient
import routing.{Version1, Version2}
import support.UnitSpec

class ApiDefinitionFactorySpec extends UnitSpec {

  class Test extends MockHttpClient with MockAppConfig {
    val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)
    MockedAppConfig.apiGatewayContext returns "individuals/disclosures"
  }


  "definition" when {
    "called" should {
      "return a valid Definition case class" in new Test {
        Seq(Version1, Version2).foreach { version =>
          MockedAppConfig.apiStatus(version).returns("BETA")
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individuals Disclosures (MTD)",
              description = "An API for providing individual disclosures data",
              context = "individuals/disclosures",
              categories = Seq("INCOME_TAX_MTD"),
              versions = Seq(
                APIVersion(
                  version = Version1,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  version = Version2,
                  status = BETA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig
          .deprecationFor(Version2)
          .returns(NotDeprecated.valid)
          .anyNumberOfTimes()
        apiDefinitionFactory.buildAPIStatus(Version2) shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "ALPHA"
        MockedAppConfig
          .deprecationFor(Version2)
          .returns(NotDeprecated.valid)
          .anyNumberOfTimes()
        apiDefinitionFactory.buildAPIStatus(Version2) shouldBe ALPHA
      }
    }

    "the 'deprecatedOn' parameter is missing for a deprecated version" should {
      "throw exception" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "DEPRECATED"
        MockedAppConfig
          .deprecationFor(Version2)
          .returns("deprecatedOn date is required for a deprecated version".invalid)
          .anyNumberOfTimes()

        val exception: Exception = intercept[Exception] {
          apiDefinitionFactory.buildAPIStatus(Version2)
        }

        val exceptionMessage: String = exception.getMessage
        exceptionMessage shouldBe "deprecatedOn date is required for a deprecated version"
      }
    }
  }
}
