/*
 * Copyright 2021 HM Revenue & Customs
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

package routing

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors.NotFoundError
import v1.stubs.{AuditStub, AuthStub, MtdIdLookupStub}

class LiveRoutesISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] = Map(
    "microservice.services.des.host" -> mockHost,
    "microservice.services.des.port" -> mockPort,
    "microservice.services.ifs.host" -> mockHost,
    "microservice.services.ifs.port" -> mockPort,
    "microservice.services.mtd-id-lookup.host" -> mockHost,
    "microservice.services.mtd-id-lookup.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "auditing.consumer.baseUri.port" -> mockPort,
    "minimumPermittedTaxYear" -> 2020,
    "feature-switch.marriage-allowance.enabled" -> false,
  )

  private trait Test {
    val nino: String = "AA111111A"

    val requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |  "spouseOrCivilPartnerNino": "AA123456A",
         |  "spouseOrCivilPartnerFirstName": "John",
         |  "spouseOrCivilPartnerSurname": "Smith",
         |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
         |}
      """.stripMargin
    )

    def uri: String = s"/marriage-allowance/$nino"

    def setupStubs(): StubMapping

    def request(uri: String): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the 'create marriage allowance' endpoint (switched on in production)" should {
    "return a 404 status code" when {
      "the feature switch is turned off to point to live routes only" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request(uri).post(requestBodyJson))
        response.body shouldBe Json.toJson(NotFoundError).toString()
        response.status shouldBe NOT_FOUND
      }
    }
  }
}