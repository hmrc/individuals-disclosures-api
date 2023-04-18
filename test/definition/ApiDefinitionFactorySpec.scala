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

import config.ConfidenceLevelConfig
import definition.APIStatus.{ ALPHA, BETA }
import definition.Versions.VERSION_1
import mocks.{ MockAppConfig, MockHttpClient }
import support.UnitSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel

class ApiDefinitionFactorySpec extends UnitSpec {

  class Test extends MockHttpClient with MockAppConfig {
    val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)
    MockAppConfig.apiGatewayContext returns "individuals/disclosures"
  }

  private val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200

  "definition" when {
    "called" should {
      "return a valid Definition case class when confidence level 200 checking is enforced" in {
        testDefinitionWithConfidence(ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = true, authValidationEnabled = true))
      }

      "return a valid Definition case class when confidence level checking 50 is enforced" in {
        testDefinitionWithConfidence(
          ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = false, authValidationEnabled = false))
      }

      def testDefinitionWithConfidence(confidenceLevelConfig: ConfidenceLevelConfig): Unit = new Test {
        MockAppConfig.apiStatus returns "1.0"
        MockAppConfig.endpointsEnabled returns true
        MockAppConfig.confidenceLevelCheckEnabled.returns(confidenceLevelConfig).anyNumberOfTimes()

        val readScope: String                = "read:self-assessment"
        val writeScope: String               = "write:self-assessment"
        val confidenceLevel: ConfidenceLevel = if (confidenceLevelConfig.authValidationEnabled) ConfidenceLevel.L200 else ConfidenceLevel.L50

        apiDefinitionFactory.definition shouldBe
          Definition(
            scopes = Seq(
              Scope(
                key = readScope,
                name = "View your Self Assessment information",
                description = "Allow read access to self assessment data",
                confidenceLevel
              ),
              Scope(
                key = writeScope,
                name = "Change your Self Assessment information",
                description = "Allow write access to self assessment data",
                confidenceLevel
              )
            ),
            api = APIDefinition(
              name = "Individuals Disclosures (MTD)",
              description = "An API for providing individual disclosures data",
              context = "individuals/disclosures",
              categories = Seq("INCOME_TAX_MTD"),
              versions = Seq(
                APIVersion(
                  version = VERSION_1,
                  status = ALPHA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "confidenceLevel" when {
    Seq(
      (true, ConfidenceLevel.L250, ConfidenceLevel.L250),
      (true, ConfidenceLevel.L200, ConfidenceLevel.L200),
      (false, ConfidenceLevel.L200, ConfidenceLevel.L50)
    ).foreach {
      case (definitionEnabled, configCL, expectedDefinitionCL) =>
        s"confidence-level-check.definition.enabled is $definitionEnabled and confidence-level = $configCL" should {
          s"return confidence level $expectedDefinitionCL" in new Test {
            MockAppConfig.confidenceLevelCheckEnabled returns ConfidenceLevelConfig(
              confidenceLevel = configCL,
              definitionEnabled = definitionEnabled,
              authValidationEnabled = true)
            apiDefinitionFactory.confidenceLevel shouldBe expectedDefinitionCL
          }
        }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockAppConfig.apiStatus returns "BETA"
        apiDefinitionFactory.buildAPIStatus(version = "1.0") shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockAppConfig.apiStatus returns "ALPHA"
        apiDefinitionFactory.buildAPIStatus(version = "1.0") shouldBe ALPHA
      }
    }
  }
}
