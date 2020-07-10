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

import play.api.libs.json.Reads

trait JsonUtils {

  /**
   * Extension methods for reads of a optional sequence
   */
  implicit class OptSeqReadsOps[A](reads: Reads[Option[Seq[A]]]) {
    /**
     * Returns a Reads that maps the sequence to itself unless it is empty
     */
    def mapEmptySeqToNone: Reads[Option[Seq[A]]] =
      reads.map {
        case Some(Nil) => None
        case other => other
      }
  }

}
