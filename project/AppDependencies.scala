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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28" % "4.2.0", //updated
    "org.typelevel" %% "cats-core"                 % "2.6.0", //updated
    "com.chuusai"   %% "shapeless"                 % "2.4.0-M1" //not changed
  )

  def test(scope: String = "test, it"): Seq[sbt.ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"          % "3.2.7"             % scope, //updated
    "com.vladsch.flexmark"   % "flexmark-all"        % "0.36.8"            % scope, // TODO
    "org.scalacheck"         %% "scalacheck"         % "1.15.3"            % scope, //not changed
    "org.scalamock"          %% "scalamock"          % "5.1.0"             % scope, //not changed
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope, //not changed
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"             % scope, //updated
    "com.github.tomakehurst" % "wiremock-jre8"       % "2.27.2"            % scope  //not changed
  )
}
