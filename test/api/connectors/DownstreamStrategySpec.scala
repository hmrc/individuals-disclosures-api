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

package api.connectors

import config.{BasicAuthDownstreamConfig, DownstreamConfig}
import org.scalatest.concurrent.ScalaFutures
import support.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class DownstreamStrategySpec extends UnitSpec with ScalaFutures {

  "StandardStrategy" must {
    "use the supplied DownstreamConfig with environment headers present" in {
      val downstreamConfig: DownstreamConfig = DownstreamConfig(
        baseUrl = "someBaseUrl",
        env = "someEnv",
        token = "someToken",
        environmentHeaders = Some(Seq("header1", "header2"))
      )

      val strategy: DownstreamStrategy = DownstreamStrategy.standardStrategy(downstreamConfig)

      strategy.baseUrl shouldBe "someBaseUrl"
      strategy.contractHeaders("someCorrelationId").futureValue should contain theSameElementsAs
        Seq(
          "Authorization" -> "Bearer someToken",
          "Environment"   -> "someEnv",
          "CorrelationId" -> "someCorrelationId"
        )
      strategy.environmentHeaders should contain theSameElementsAs Seq("header1", "header2")
    }

    "use the supplied DownstreamConfig with environment headers absent" in {
      val downstreamConfig: DownstreamConfig = DownstreamConfig(
        baseUrl = "someBaseUrl",
        env = "someEnv",
        token = "someToken",
        environmentHeaders = None
      )

      val strategy: DownstreamStrategy = DownstreamStrategy.standardStrategy(downstreamConfig)

      strategy.baseUrl shouldBe "someBaseUrl"
      strategy.contractHeaders("someCorrelationId").futureValue should contain theSameElementsAs
        Seq(
          "Authorization" -> "Bearer someToken",
          "Environment"   -> "someEnv",
          "CorrelationId" -> "someCorrelationId"
        )
      strategy.environmentHeaders shouldBe empty
    }
  }

  "BasicAuthStrategy" must {
    "use the supplied BasicAuthDownstreamConfig with environment headers present" in {
      val downstreamConfig: BasicAuthDownstreamConfig = BasicAuthDownstreamConfig(
        baseUrl = "someBaseUrl",
        env = "someEnv",
        clientId = "someClient",
        clientSecret = "someSecret",
        environmentHeaders = Some(Seq("header1", "header2"))
      )

      val strategy: DownstreamStrategy = DownstreamStrategy.basicAuthStrategy(downstreamConfig)

      strategy.baseUrl shouldBe "someBaseUrl"
      strategy.contractHeaders("someCorrelationId").futureValue should contain theSameElementsAs
        Seq(
          "Authorization" -> "Basic c29tZUNsaWVudDpzb21lU2VjcmV0",
          "Environment"   -> "someEnv",
          "CorrelationId" -> "someCorrelationId"
        )
      strategy.environmentHeaders should contain theSameElementsAs Seq("header1", "header2")

    }

    "use the supplied BasicAuthDownstreamConfig with environment headers absent" in {
      val downstreamConfig: BasicAuthDownstreamConfig = BasicAuthDownstreamConfig(
        baseUrl = "someBaseUrl",
        env = "someEnv",
        clientId = "someClient",
        clientSecret = "someSecret",
        environmentHeaders = None
      )

      val strategy: DownstreamStrategy = DownstreamStrategy.basicAuthStrategy(downstreamConfig)

      strategy.baseUrl shouldBe "someBaseUrl"
      strategy.contractHeaders("someCorrelationId").futureValue should contain theSameElementsAs
        Seq(
          "Authorization" -> "Basic c29tZUNsaWVudDpzb21lU2VjcmV0",
          "Environment"   -> "someEnv",
          "CorrelationId" -> "someCorrelationId"
        )
      strategy.environmentHeaders shouldBe empty
    }
  }

}
