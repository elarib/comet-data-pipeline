/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.ebiznext.comet.schema.model

import java.io.{InputStream, StringWriter}

import com.ebiznext.comet.TestHelper

class SchemaSpec extends TestHelper {

  "Attribute type" should "be valid" in {
    val stream: InputStream =
      getClass.getResourceAsStream("/sample/default.yml")
    val lines =
      scala.io.Source.fromInputStream(stream).getLines().mkString("\n")
    val types = mapper.readValue(lines, classOf[Types])
    val attr = Attribute(
      "attr",
      "invalid-type", // should raise error non existent type
      Some(true),
      true,
      Some(PrivacyLevel("MD5")) // Should raise an error. Privacy cannot be applied on types other than string
    )

    attr.checkValidity() shouldBe Left(List("Invalid Type invalid-type"))
  }

  "Attribute privacy" should "be applied on string type only" in {
    val attr = Attribute(
      "attr",
      "long",
      Some(true),
      true,
      Some(PrivacyLevel("MD5")) // Should raise an error. Privacy cannot be applied on types other than stringsettings = settings
    )
    attr.checkValidity() shouldBe
    Left(
      List(
        "Attribute Attribute(attr,long,Some(true),true,Some(MD5),None,None,None,None,None,None,None) : string is the only supported primitive type for an attribute when privacy is requested"
      )
    )
  }

  "Sub Attribute" should "be present for struct types only" in {
    val attr = Attribute(
      "attr",
      "long",
      Some(true),
      true,
      Some(PrivacyLevel("MD5")), // Should raise an error. Privacy cannot be applied on types other than string
      attributes = Some(List[Attribute]())
    )
    val expectedErrors = List(
      "Attribute Attribute(attr,long,Some(true),true,Some(MD5),None,None,None,Some(List()),None,None,None) : string is the only supported primitive type for an attribute when privacy is requested",
      "Attribute Attribute(attr,long,Some(true),true,Some(MD5),None,None,None,Some(List()),None,None,None) : Simple attributes cannot have sub-attributes",
      "Attribute Attribute(attr,long,Some(true),true,Some(MD5),None,None,None,Some(List()),None,None,None) : when present, attributes list cannot be empty."
    )

    attr.checkValidity() shouldBe Left(expectedErrors)
  }

  "Position serialization" should "output all fields" in {
    val yml = loadFile(s"/expected/yml/position_serialization_${versionSuffix}.yml")

    val attr =
      Attribute("hello", position = Some(Position(1, 2, Some(Trim.NONE))))
    val writer = new StringWriter()
    mapper.writer().writeValue(writer, attr)
    logger.info("--" + writer.toString + "--")
    logger.info("++" + yml + "++")
    writer.toString.trim should equal(yml)
  }

  "Default value for an attribute" should "only be used for non obligatory fields" in {
    val requiredAttribute =
      Attribute("requiredAttribute", "long", required = true, default = Some("10"))
    requiredAttribute.checkValidity() shouldBe Left(
      List(
        s"attribute with name ${requiredAttribute.name}: default value valid for optional fields only"
      )
    )

    val optionalAttribute =
      Attribute("optionalAttribute", "long", required = false, default = Some("10"))
    optionalAttribute.checkValidity() shouldBe Right(true)
  }
}
