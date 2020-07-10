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

package utils

import play.api.libs.json._
import support.UnitSpec

class JsonUtilsSpec extends UnitSpec with JsonUtils {

  "mapEmptySeqToNone" must {
    val reads = __.readNullable[Seq[String]].mapEmptySeqToNone

    "map non-empty sequence to Some(non-empty sequence)" in {
      JsArray(Seq(JsString("value0"), JsString("value1"))).as(reads) shouldBe Some(Seq("value0", "value1"))
    }

    "map empty sequence to None" in {
      JsArray.empty.as(reads) shouldBe None
    }

    "map None to None" in {
      JsNull.as(reads) shouldBe None
    }
  }
}