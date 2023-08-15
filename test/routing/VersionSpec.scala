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
import play.api.libs.json._
import support.UnitSpec

class VersionSpec extends UnitSpec {

  "VersionReads" when {
    "reading a valid version string" should {
      "return Version1" in {
        JsString(Version1.name).validate[Version](Version.VersionReads) shouldBe JsSuccess(Version1)
      }
    }
    "reading an invalid version string" should {
      "return a JsError" in {
        JsString("unknown").validate[Version](Version.VersionReads) shouldBe a[JsError]
      }
    }
  }

  "Versions" when {
    "retrieved from a request header" should {
      "return Version1 for valid header" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))) shouldBe Right(Version1)
      }
      "return InvalidHeader when the version header is missing" in {
        Versions.getFromRequest(FakeRequest().withHeaders()) shouldBe Left(InvalidHeader)
      }
      "return VersionNotFound for unrecognised version" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))) shouldBe Left(VersionNotFound)
      }
      "return InvalidHeader for a header format that doesn't match regex" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "invalidHeaderFormat"))) shouldBe Left(InvalidHeader)
      }
    }
  }

}
