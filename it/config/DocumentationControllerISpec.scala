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

package config

import io.swagger.v3.parser.OpenAPIV3Parser
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import routing.Version1
import support.IntegrationBaseSpec

import scala.util.Try

class DocumentationControllerISpec extends IntegrationBaseSpec {

  private val apiDefinitionJson = Json.parse(
    s"""
    |{
    |   "api":{
    |      "name":"Individuals Disclosures (MTD)",
    |      "description":"An API for providing individual disclosures data",
    |      "context":"individuals/disclosures",
    |      "categories":[
    |         "INCOME_TAX_MTD"
    |      ],
    |      "versions":[
    |         {
    |            "version":"1.0",
    |            "status":"BETA",
    |            "endpointsEnabled":true
    |         }
    |      ]
    |   }
    |}
    """.stripMargin
  )

  "GET /api/definition" should {
    "return a 200 with the correct response body" in {
      val response: WSResponse = await(buildRequest("/api/definition").get())
      response.status shouldBe Status.OK
      Json.parse(response.body) shouldBe apiDefinitionJson
    }
  }

  "an OAS documentation request" must {
    List(Version1).foreach { version =>
      s"return the documentation for $version" in {
        val response = get(s"/api/conf/${version.name}/application.yaml")

        val body         = response.body
        val parserResult = Try(new OpenAPIV3Parser().readContents(body)).getOrElse(fail("openAPI couldn't read contents"))

        val openAPI = Option(parserResult.getOpenAPI).getOrElse(fail("openAPI wasn't defined"))
        openAPI.getOpenapi shouldBe "3.0.3"
        withClue(s"If v${version.name} endpoints are enabled in application.conf, remove the [test only] from this test: ") {
          openAPI.getInfo.getTitle shouldBe "Individuals Disclosures (MTD)"
        }

        openAPI.getInfo.getVersion shouldBe version.toString
      }

      s"return the documentation with the correct accept header for version $version" in {
        val response = get(s"/api/conf/${version.name}/common/headers.yaml")
        val body     = response.body

        val headerRegex = """(?s).*?application/vnd\.hmrc\.(\d+\.\d+)\+json.*?""".r
        val header      = headerRegex.findFirstMatchIn(body)
        header.isDefined shouldBe true

        val versionFromHeader = header.get.group(1)
        versionFromHeader shouldBe version.name
      }
    }
  }

  private def get(path: String): WSResponse = {
    val response: WSResponse = await(buildRequest(path).get())
    response.status shouldBe OK
    response
  }

}
