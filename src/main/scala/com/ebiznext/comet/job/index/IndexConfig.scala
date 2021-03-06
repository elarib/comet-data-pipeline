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

package com.ebiznext.comet.job.index

import java.util.regex.Pattern

import com.ebiznext.comet.config.Settings
import org.apache.hadoop.fs.Path
import scopt.OParser

case class IndexConfig(
  timestamp: Option[String] = None,
  id: Option[String] = None,
  mapping: Option[Path] = None,
  domain: String = "",
  schema: String = "",
  format: String = "",
  dataset: Option[Path] = None,
  conf: Map[String, String] = Map()
) {

  def getDataset()(implicit settings: Settings): Path = {
    dataset.getOrElse {
      new Path(s"${settings.comet.datasets}/${settings.comet.area.accepted}/$domain/$schema")
    }
  }

  def getIndexName(): String = s"${domain}_$schema"

  def getTypeName(): String = s"${domain}_$schema"

  private val pattern = Pattern.compile("\\{(.*)\\|(.*)\\}")

  def getTimestampCol(): Option[String] = {
    timestamp.flatMap { ts =>
      val matcher = pattern.matcher(ts)
      if (matcher.matches()) {
        Some(matcher.group(1))
      } else {
        None
      }
    }
  }

  def getResource(): String = {
    timestamp.map { ts =>
      s"${this.getIndexName()}-$ts/_doc"
    } getOrElse {
      s"${this.getIndexName()}/_doc"
    }
  }
}

object IndexConfig {

  def parse(args: Seq[String]): Option[IndexConfig] = {
    val builder = OParser.builder[IndexConfig]
    val parser: OParser[Unit, IndexConfig] = {
      import builder._
      OParser.sequence(
        programName("comet"),
        head("comet", "1.x"),
        opt[String]("timestamp")
          .action((x, c) => c.copy(timestamp = Some(x)))
          .optional()
          .text("Elasticsearch index timestamp suffix as in {@timestamp|yyyy.MM.dd}"),
        opt[String]("id")
          .action((x, c) => c.copy(id = Some(x)))
          .optional()
          .text("Elasticsearch Document Id"),
        opt[String]("mapping")
          .action((x, c) => c.copy(mapping = Some(new Path(x))))
          .optional()
          .text("Path to Elasticsearch Mapping File"),
        opt[String]("domain")
          .action((x, c) => c.copy(domain = x))
          .required()
          .text("Domain Name"),
        opt[String]("schema")
          .action((x, c) => c.copy(schema = x))
          .required()
          .text("Schema Name"),
        opt[String]("format")
          .action((x, c) => c.copy(format = x))
          .required()
          .text("Dataset input file : parquet, json or json-array"),
        opt[String]("dataset")
          .action((x, c) => c.copy(dataset = Some(new Path(x))))
          .optional()
          .text("Input dataset path"),
        opt[Map[String, String]]("conf")
          .action((x, c) => c.copy(conf = x))
          .optional()
          .valueName(
            "es.batch.size.entries=1000,es.batch.size.bytes=1mb... (see https://www.elastic.co/guide/en/elasticsearch/hadoop/current/configuration.html)"
          )
          .text("eshadoop configuration options")
      )
    }
    OParser.parse(parser, args, IndexConfig())
  }
}
