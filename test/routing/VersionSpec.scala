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

package routing

import play.api.http.HeaderNames.ACCEPT
import play.api.test.FakeRequest
import play.api.libs.json.*
import play.api.mvc.{Headers, RequestHeader}
import support.UnitSpec

class VersionSpec extends UnitSpec {

  "VersionReads" when {
    "reading a valid version string" should {
      "return Version1" in {
        JsString(Version1.name).validate[Version](Version.VersionReads) shouldBe JsSuccess(Version1)
      }
      "return Version2" in {
        JsString(Version2.name).validate[Version](Version.VersionReads) shouldBe JsSuccess(Version2)
      }
    }
    "reading an invalid version string" should {
      "return a JsError" in {
        JsString("unknown").validate[Version](Version.VersionReads) shouldBe a[JsError]
      }
    }
  }

  "Version.apply(RequestHeader)" when {

    def mockRequestHeader(keyValue: (String, String)): RequestHeader = {
      val (k, v)  = keyValue
      val header  = mock[RequestHeader]
      val headers = Headers(k -> v)

      (() => header.headers: Headers).expects().returning(headers)
      header
    }

    "given a valid Accept header" should {
      "return the expected API Version" in {
        val header = mockRequestHeader(ACCEPT -> "application/vnd.hmrc.2.0+json")
        val result = Version(header)
        result shouldBe Version2
      }
    }

    "given an invalid Accept header" should {
      "throw the expected exception (code shouldn't have reached this point)" in {
        val header = mockRequestHeader(ACCEPT -> "not-a-valid-request-header")
        the[Exception] thrownBy Version(header) should have message "Missing or unsupported version found in request accept header"
      }
    }
  }

  "Versions" when {
    "retrieved from a request header" should {
      "return Version1 for valid header" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))) shouldBe Right(Version1)
      }
      "return Version2 for valid header" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))) shouldBe Right(Version2)
      }
      "return InvalidHeader when the version header is missing" in {
        Versions.getFromRequest(FakeRequest().withHeaders()) shouldBe Left(InvalidHeader)
      }
      "return VersionNotFound for unrecognised version" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))) shouldBe Left(VersionNotFound)
      }
      "return InvalidHeader for a header format that doesn't match regex" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "invalidHeaderFormat"))) shouldBe Left(InvalidHeader)
      }
    }
  }

}
